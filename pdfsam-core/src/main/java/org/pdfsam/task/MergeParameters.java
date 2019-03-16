package org.pdfsam.task;

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.sejda.model.input.MergeInput;
import org.sejda.model.input.PdfMergeInput;
import org.sejda.model.outline.CatalogPageLabelsPolicy;
import org.sejda.model.outline.OutlinePolicy;
import org.sejda.model.output.SingleTaskOutput;
import org.sejda.model.parameter.base.AbstractPdfOutputParameters;
import org.sejda.model.parameter.base.SingleOutputTaskParameters;
import org.sejda.model.pdf.form.AcroFormPolicy;
import org.sejda.model.toc.ToCPolicy;
import org.sejda.model.validation.constraint.NotEmpty;
import org.sejda.model.validation.constraint.SingleOutputAllowedExtensions;

/**
 * Parameter class for a merge task containing a collection of input to be merged.
 *
 * @author Andrea Vacondio
 *
 */
@SingleOutputAllowedExtensions
public class MergeParameters extends AbstractPdfOutputParameters implements SingleOutputTaskParameters {

    @NotEmpty
    @Valid
    private List<MergeInput> inputList = new ArrayList<>();
    private boolean blankPageIfOdd = false;
    @NotNull
    private OutlinePolicy outlinePolicy = OutlinePolicy.RETAIN;
    @NotNull
    private AcroFormPolicy acroFormPolicy = AcroFormPolicy.MERGE_RENAMING_EXISTING_FIELDS;
    @NotNull
    private CatalogPageLabelsPolicy catalogPageLabelsPolicy = CatalogPageLabelsPolicy.DISCARD;
    @Valid
    @NotNull
    private SingleTaskOutput output;
    @NotNull
    private ToCPolicy tocPolicy = ToCPolicy.NONE;
    private boolean filenameFooter = false;
    /* Makes all pages same width as the first page */
    private boolean normalizePageSizes = false;
    private boolean firstInputCoverTitle = false;

    @Override
    public SingleTaskOutput getOutput() {
        return output;
    }

    @Override
    public void setOutput(SingleTaskOutput output) {
        this.output = output;
    }

    /**
     * @return an unmodifiable view of the inputList
     */
    public List<MergeInput> getInputList() {
        return Collections.unmodifiableList(inputList);
    }

    public void setInputList(List<MergeInput> inputList) {
        this.inputList = inputList;
    }

    public List<PdfMergeInput> getPdfInputList() {
        return Collections.unmodifiableList(
                inputList.stream().filter(input -> input instanceof PdfMergeInput)
                        .map(input -> (PdfMergeInput) input).collect(Collectors.toList())
        );
    }

    /**
     * adds the given input to the list of inputs for the merge task.
     *
     * @param input
     */
    public void addInput(MergeInput input) {
        this.inputList.add(input);
    }

    public boolean isBlankPageIfOdd() {
        return blankPageIfOdd;
    }

    /**
     * Setting this true tells the task to add a blank page after each merged document if the number of pages is odd. It can be useful to print the document double-sided.
     *
     * @param blankPageIfOdd
     */
    public void setBlankPageIfOdd(boolean blankPageIfOdd) {
        this.blankPageIfOdd = blankPageIfOdd;
    }

    public AcroFormPolicy getAcroFormPolicy() {
        return this.acroFormPolicy;
    }

    /**
     * The policy that the merge task should use when handling interactive forms (AcroForm)
     *
     * @param acroFormPolicy
     */
    public void setAcroFormPolicy(AcroFormPolicy acroFormPolicy) {
        this.acroFormPolicy = acroFormPolicy;
    }

    public OutlinePolicy getOutlinePolicy() {
        return outlinePolicy;
    }

    /**
     * The policy that the merge task should use when handling the outline tree (bookmarks)
     *
     * @param outlinePolicy
     */
    public void setOutlinePolicy(OutlinePolicy outlinePolicy) {
        this.outlinePolicy = outlinePolicy;
    }

    public ToCPolicy getTableOfContentsPolicy() {
        return ofNullable(tocPolicy).orElse(ToCPolicy.NONE);
    }

    public void setTableOfContentsPolicy(ToCPolicy tocPolicy) {
        this.tocPolicy = tocPolicy;
    }

    public boolean isFilenameFooter() {
        return filenameFooter;
    }

    public void setFilenameFooter(boolean filenameFooter) {
        this.filenameFooter = filenameFooter;
    }

    public boolean isNormalizePageSizes() {
        return normalizePageSizes;
    }

    public void setNormalizePageSizes(boolean normalizePageSizes) {
        this.normalizePageSizes = normalizePageSizes;
    }

    public CatalogPageLabelsPolicy getCatalogPageLabelsPolicy() {
        return catalogPageLabelsPolicy;
    }

    public void setCatalogPageLabelsPolicy(CatalogPageLabelsPolicy catalogPageLabelsPolicy) {
        this.catalogPageLabelsPolicy = catalogPageLabelsPolicy;
    }

    public boolean isFirstInputCoverTitle() {
        return firstInputCoverTitle;
    }

//    public void setFirstInputCoverTitle(boolean firstInputCoverTitle) {
//        this.firstInputCoverTitle = firstInputCoverTitle;
//    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).append(inputList).append(acroFormPolicy)
                .append(blankPageIfOdd).append(outlinePolicy).append(tocPolicy).append(output).append(filenameFooter)
                .append(normalizePageSizes).append(catalogPageLabelsPolicy).append(firstInputCoverTitle)
                .toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof org.sejda.model.parameter.MergeParameters)) {
            return false;
        }
        org.sejda.model.parameter.MergeParameters params = (org.sejda.model.parameter.MergeParameters) other;
        return new EqualsBuilder().appendSuper(super.equals(other)).append(inputList, params.inputList)
                .append(acroFormPolicy, params.getAcroFormPolicy()).append(blankPageIfOdd, params.isBlankPageIfOdd())
                .append(outlinePolicy, params.getOutlinePolicy()).append(tocPolicy, params.getTableOfContentsPolicy())
                .append(output, params.getOutput()).append(filenameFooter, params.isFilenameFooter())
                .append(normalizePageSizes, params.isNormalizePageSizes())
                .append(catalogPageLabelsPolicy, params.catalogPageLabelsPolicy)
                .append(firstInputCoverTitle, params.firstInputCoverTitle)
                .isEquals();
    }
}
