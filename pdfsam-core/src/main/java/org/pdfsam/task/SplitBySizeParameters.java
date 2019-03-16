package org.pdfsam.task;

//import javax.validation.constraints.Min;
//import javax.validation.constraints.NotNull;

//import org.apache.commons.lang3.builder.EqualsBuilder;
//import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.sejda.model.optimization.OptimizationPolicy;
import org.sejda.model.parameter.base.DiscardableOutlineTaskParameters;
import org.sejda.model.parameter.base.MultiplePdfSourceMultipleOutputParameters;
import org.sejda.model.parameter.base.OptimizableOutputTaskParameters;


/**
 * Parameter class for a split by size task.
 *
 * @author Andrea Vacondio
 *
 */
public class SplitBySizeParameters extends MultiplePdfSourceMultipleOutputParameters
        implements OptimizableOutputTaskParameters, DiscardableOutlineTaskParameters {

    @NotNull
    private OptimizationPolicy optimizationPolicy = OptimizationPolicy.NO;


    @Min(1)
    private long sizeToSplitAt;
    private boolean discardOutline = false;

    public SplitBySizeParameters(long sizeToSplitAt) {
        this.sizeToSplitAt = sizeToSplitAt;
    }

    public long getSizeToSplitAt() {
        return sizeToSplitAt;
    }

    @Override
    public OptimizationPolicy getOptimizationPolicy() {
        return optimizationPolicy;
    }

    @Override
    public void setOptimizationPolicy(OptimizationPolicy optimizationPolicy) {
        this.optimizationPolicy = optimizationPolicy;
    }

    @Override
    public boolean discardOutline() {
        return discardOutline;
    }

    @Override
    public void discardOutline(boolean discardOutline) {
        this.discardOutline = discardOutline;
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode()).append(optimizationPolicy).append(sizeToSplitAt)
                .append(discardOutline).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof org.sejda.model.parameter.SplitBySizeParameters)) {
            return false;
        }
        org.sejda.model.parameter.SplitBySizeParameters parameter = (org.sejda.model.parameter.SplitBySizeParameters) other;
        return new EqualsBuilder().appendSuper(super.equals(other))
                .append(optimizationPolicy, parameter.getOptimizationPolicy())
                .append(sizeToSplitAt, parameter.getSizeToSplitAt()).append(discardOutline, parameter.discardOutline)
                .isEquals();
    }
}
