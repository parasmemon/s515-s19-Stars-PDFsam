package org.pdfsam.task;

import org.sejda.core.support.io.OutputWriters;
import org.sejda.core.support.io.SingleOutputWriter;
import org.sejda.impl.sambox.component.PdfAlternateMixer;
import org.sejda.model.exception.TaskException;
import org.sejda.model.task.BaseTask;
import org.sejda.model.task.TaskExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.sejda.common.ComponentsUtility.nullSafeCloseQuietly;
import static org.sejda.core.support.io.IOUtils.createTemporaryBuffer;

//import org.sejda.model.parameter.AlternateMixMultipleInputParameters;

/**
 * SAMBox implementation of the AlternateMix task performing the mix of two given {@link org.sejda.model.input.PdfMixInput}s.
 *
 * @author Andrea Vacondio
 */
public class AlternateMixTask extends BaseTask<AlternateMixMultipleInputParameters> {

    private static final Logger LOG = LoggerFactory.getLogger(AlternateMixTask.class);

    private PdfAlternateMixer mixer = null;
    private SingleOutputWriter outputWriter;

    @Override
    public void before(AlternateMixMultipleInputParameters parameters, TaskExecutionContext executionContext)
            throws TaskException {
        super.before(parameters, executionContext);
        mixer = new PdfAlternateMixer();
        outputWriter = OutputWriters.newSingleOutputWriter(parameters.getExistingOutputPolicy(), executionContext);
    }

    @Override
    public void execute(AlternateMixMultipleInputParameters parameters) throws TaskException {

        LOG.debug("Starting alternate mix of {} input documents", parameters.getInputList().size());
        mixer.mix(parameters.getInputList(), executionContext());
        mixer.setVersionOnPDDocument(parameters.getVersion());
        mixer.setCompress(parameters.isCompress());

        File tmpFile = createTemporaryBuffer(parameters.getOutput());
        outputWriter.taskOutput(tmpFile);
        LOG.debug("Temporary output set to {}", tmpFile);
        mixer.savePDDocument(tmpFile);
        nullSafeCloseQuietly(mixer);

        parameters.getOutput().accept(outputWriter);

        LOG.debug("Alternate mix of {} files completed", parameters.getInputList().size());
    }

    @Override
    public void after() {
        nullSafeCloseQuietly(mixer);
    }

}

