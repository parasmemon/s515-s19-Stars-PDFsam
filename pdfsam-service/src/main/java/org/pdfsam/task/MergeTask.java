package org.pdfsam.task;

import static java.util.Optional.ofNullable;
import static org.sejda.common.ComponentsUtility.nullSafeCloseQuietly;
import static org.sejda.core.notification.dsl.ApplicationEventsNotifier.notifyEvent;
import static org.sejda.core.support.io.IOUtils.createTemporaryBufferWithName;
import static org.sejda.impl.sambox.component.SignatureClipper.clipSignatures;

import java.io.Closeable;
import java.io.File;
import java.util.*;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.sejda.common.LookupTable;
import org.sejda.core.support.io.IOUtils;
import org.sejda.core.support.io.OutputWriters;
import org.sejda.core.support.io.SingleOutputWriter;
import org.sejda.impl.sambox.component.*;
import org.sejda.impl.sambox.component.image.ImagesToPdfDocumentConverter;
import org.sejda.model.exception.TaskException;
import org.sejda.model.input.*;

//import org.sejda.model.input.PdfMergeInput;

import org.pdfsam.task.PdfMergeInput;

//import org.sejda.model.parameter.MergeParameters;

//import org.pdfsam.task.MergeParameters;

import org.sejda.model.scale.ScaleType;
import org.sejda.model.task.BaseTask;
import org.sejda.model.task.TaskExecutionContext;
import org.sejda.model.toc.ToCPolicy;
import org.sejda.sambox.pdmodel.PDPage;
import org.sejda.sambox.pdmodel.PageNotFoundException;
import org.sejda.sambox.pdmodel.common.PDRectangle;
import org.sejda.sambox.pdmodel.interactive.annotation.PDAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SAMBox implementation of the Merge task that merges together a number of documents or part of them.
 *
 * @author Andrea Vacondio
 *
 */
public class MergeTask extends BaseTask<MergeParameters> {

    private static final Logger LOG = LoggerFactory.getLogger(org.sejda.impl.sambox.MergeTask.class);

    private SingleOutputWriter outputWriter;
    private PdfSourceOpener<PDDocumentHandler> sourceOpener;
    private int totalSteps;
    private PDDocumentHandler destinationDocument;
    private Queue<Closeable> toClose = new LinkedList<>();
    private OutlineMerger outlineMerger;
    private CatalogPageLabelsMerger catalogPageLabelsMerger;
    private AcroFormsMerger acroFormsMerger;
    private TableOfContentsCreator tocCreator;
    private FilenameFooterWriter footerWriter;
    private PDRectangle currentPageSize = PDRectangle.A4;
    private long pagesCounter = 0;
    private long inputsCounter = 0;
    private int firstInputNumberOfPages = 0;

    @Override
    public void before(MergeParameters parameters, TaskExecutionContext executionContext) throws TaskException {
        super.before(parameters, executionContext);
        totalSteps = parameters.getInputList().size();
        sourceOpener = new DefaultPdfSourceOpener();
        outputWriter = OutputWriters.newSingleOutputWriter(parameters.getExistingOutputPolicy(), executionContext);
        outlineMerger = new OutlineMerger(parameters.getOutlinePolicy());
    }

    @Override
    public void execute(MergeParameters parameters) throws TaskException {
        int currentStep = 0;
        File tmpFile = IOUtils.createTemporaryBuffer(parameters.getOutput());
        outputWriter.taskOutput(tmpFile);
        LOG.debug("Temporary output set to {}", tmpFile);

        this.destinationDocument = new PDDocumentHandler();
        this.destinationDocument.setCreatorOnPDDocument();
        this.destinationDocument.setVersionOnPDDocument(parameters.getVersion());
        this.destinationDocument.setCompress(parameters.isCompress());
        this.acroFormsMerger = new AcroFormsMerger(parameters.getAcroFormPolicy(),
                this.destinationDocument.getUnderlyingPDDocument());
        this.tocCreator = new TableOfContentsCreator(parameters, this.destinationDocument.getUnderlyingPDDocument());
        this.footerWriter = new FilenameFooterWriter(parameters.isFilenameFooter(),
                this.destinationDocument.getUnderlyingPDDocument());
        this.catalogPageLabelsMerger = new CatalogPageLabelsMerger(parameters.getCatalogPageLabelsPolicy());

        convertImageMergeInputToPdf(parameters);

        for (org.sejda.model.input.PdfMergeInput input : parameters.getPdfInputList()) {
            inputsCounter++;
            LOG.debug("Opening {}", input.getSource());
            PDDocumentHandler sourceDocumentHandler = input.getSource().open(sourceOpener);
            toClose.add(sourceDocumentHandler);

            if(inputsCounter == 1) {
                firstInputNumberOfPages = sourceDocumentHandler.getNumberOfPages();
            }

            LOG.debug("Adding pages");
            LookupTable<PDPage> pagesLookup = new LookupTable<>();
            long relativePagesCounter = 0;
            Set<Integer> pagesToImport = input.getPages(sourceDocumentHandler.getNumberOfPages());
            for (Integer currentPage : pagesToImport) {
                executionContext().assertTaskNotCancelled();
                pagesCounter++;
                relativePagesCounter++;
                try {
                    PDPage page = sourceDocumentHandler.getPage(currentPage);
                    // we keep rotation into account
                    currentPageSize = page.getMediaBox().rotate(page.getRotation());
                    // we don't use the original page because once added to the new tree we loose inheritable attributes
                    // so we use a page duplicate to explicitly assign inheritable resources
                    PDPage importedPage = destinationDocument.importPage(page);
                    pagesLookup.addLookupEntry(page, importedPage);

                    String sourceBaseName = FilenameUtils.getBaseName(input.getSource().getName());

                    // processing the first page of the source
                    if (tocCreator.shouldGenerateToC() && relativePagesCounter == 1) {
                        if(parameters.isFirstInputCoverTitle() && inputsCounter == 1) {
                            // skip the cover/title document, don't add it to the ToC
                        } else {
                            tocCreator.pageSizeIfNotSet(currentPageSize);
                            if (ToCPolicy.DOC_TITLES == parameters.getTableOfContentsPolicy()) {
                                sourceBaseName = ofNullable(
                                        sourceDocumentHandler.getUnderlyingPDDocument().getDocumentInformation())
                                        .map(i -> i.getTitle()).filter(StringUtils::isNotBlank)
                                        .orElse(sourceBaseName);
                            }
                            tocCreator.appendItem(sourceBaseName, pagesCounter, importedPage);
                        }
                    }

                    long currentPageNumber = pagesCounter + tocCreator.tocNumberOfPages();
                    if(parameters.isFirstInputCoverTitle() && inputsCounter == 1) {
                        // the toc will be added after the cover/title pages
                        currentPageNumber = pagesCounter;
                    }
                    this.footerWriter.addFooter(importedPage, sourceBaseName, currentPageNumber);
                    LOG.trace("Added imported page");
                } catch (PageNotFoundException e) {
                    executionContext().assertTaskIsLenient(e);
                    notifyEvent(executionContext().notifiableTaskMetadata())
                            .taskWarning(String.format("Page %d was skipped, could not be processed", currentPage), e);
                }
            }
            relativePagesCounter = 0;

            outlineMerger.updateOutline(sourceDocumentHandler.getUnderlyingPDDocument(), input.getSource().getName(),
                    pagesLookup);

            LookupTable<PDAnnotation> annotationsLookup = new AnnotationsDistiller(
                    sourceDocumentHandler.getUnderlyingPDDocument()).retainRelevantAnnotations(pagesLookup);
            clipSignatures(annotationsLookup.values());

            acroFormsMerger.mergeForm(
                    sourceDocumentHandler.getUnderlyingPDDocument().getDocumentCatalog().getAcroForm(),
                    annotationsLookup);

            if (parameters.isBlankPageIfOdd()) {
                ofNullable(destinationDocument.addBlankPageIfOdd(currentPageSize)).ifPresent(p -> pagesCounter++);
            }

            catalogPageLabelsMerger.add(sourceDocumentHandler.getUnderlyingPDDocument(), pagesToImport);

            notifyEvent(executionContext().notifiableTaskMetadata()).stepsCompleted(++currentStep).outOf(totalSteps);
        }

        if (outlineMerger.hasOutline()) {
            LOG.debug("Adding generated outline");
            destinationDocument.setDocumentOutline(outlineMerger.getOutline());
        }

        ofNullable(acroFormsMerger.getForm()).filter(f -> !f.getFields().isEmpty()).ifPresent(f -> {
            LOG.debug("Adding generated AcroForm");
            destinationDocument.setDocumentAcroForm(f);
        });

        if (parameters.isNormalizePageSizes()) {
            LOG.debug("Normalizing page widths to match width of first page");
            // Do this before generating TOC, so the first page is from content.
            new PdfScaler(ScaleType.PAGE).scalePages(destinationDocument.getUnderlyingPDDocument());
        }

        if (tocCreator.hasToc()) {
            LOG.debug("Adding generated ToC");
            int beforePageNumber = 0;
            if(parameters.isFirstInputCoverTitle()) {
                // add ToC after the cover/title pages
                beforePageNumber = firstInputNumberOfPages;
            }
            tocCreator.addToC(beforePageNumber);
        }

        if(catalogPageLabelsMerger.hasPageLabels()) {
            LOG.debug("Adding merged /Catalog /PageLabels");
            destinationDocument.getUnderlyingPDDocument().getDocumentCatalog().setPageLabels(catalogPageLabelsMerger.getMergedPageLabels());
        }

        destinationDocument.savePDDocument(tmpFile);
        closeResources();

        parameters.getOutput().accept(outputWriter);
        LOG.debug("Input documents merged correctly and written to {}", parameters.getOutput());

    }

    private void convertImageMergeInputToPdf(MergeParameters parameters) throws TaskException {
        // if images were supplied, convert them to PDF
        List<MergeInput> newInputList = new ArrayList<>();
        for (MergeInput input : parameters.getInputList()) {
            if (input instanceof ImageMergeInput) {
                // collect all consecutive images and convert them to a PDF document
                newInputList.add(convertImagesToPdfMergeInput((ImageMergeInput) input));
            } else {
                newInputList.add(input);
            }
        }

        parameters.setInputList(newInputList);
    }

    private org.sejda.model.input.PdfMergeInput convertImagesToPdfMergeInput(ImageMergeInput image) throws TaskException {
        List<Source<?>> sources = Collections.singletonList(image.getSource());
        PDDocumentHandler converted = new ImagesToPdfDocumentConverter().convert(sources);
        String basename = FilenameUtils.getBaseName(image.getSource().getName());
        String filename = String.format("%s.pdf", basename);
        File convertedTmpFile = createTemporaryBufferWithName(filename);
        converted.setDocumentTitle(basename);
        converted.savePDDocument(convertedTmpFile);
        return new PdfMergeInput(PdfFileSource.newInstanceNoPassword(convertedTmpFile));
    }

    private void closeResources() {
        Closeable current;
        while ((current = toClose.poll()) != null) {
            nullSafeCloseQuietly(current);
        }
        nullSafeCloseQuietly(destinationDocument);
    }

    @Override
    public void after() {
        closeResources();
        outputWriter = null;
    }

}
