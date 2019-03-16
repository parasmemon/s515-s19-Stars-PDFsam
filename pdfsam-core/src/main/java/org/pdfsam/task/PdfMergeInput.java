package org.pdfsam.task;


import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.sejda.common.collection.NullSafeSet;
import org.sejda.model.input.MergeInput;
import org.sejda.model.input.PdfSource;
import org.sejda.model.pdf.page.PageRange;
import org.sejda.model.pdf.page.PageRangeSelection;
import org.sejda.model.pdf.page.PagesSelection;
import org.sejda.model.validation.constraint.NoIntersections;

/**
 * Model for a input source for a merge task. It contains the source and the page selection on the source.
 *
 * @author Andrea Vacondio
 *
 */
@NoIntersections
public class PdfMergeInput implements PageRangeSelection, PagesSelection, MergeInput {

    @NotNull
    @Valid
    private PdfSource<?> source;
    @Valid
    private final Set<PageRange> pageSelection = new NullSafeSet<>();

    public PdfMergeInput(PdfSource<?> source, Set<PageRange> pageSelection) {
        this.source = source;
        this.pageSelection.addAll(pageSelection);
    }

    public PdfMergeInput(PdfSource<?> source) {
        this.source = source;
    }

    public PdfSource<?> getSource() {
        return source;
    }

    /**
     * @return an unmodifiable view of the pageSelection
     */
    @Override
    public Set<PageRange> getPageSelection() {
        return Collections.unmodifiableSet(pageSelection);
    }

    public void addPageRange(PageRange range) {
        pageSelection.add(range);
    }

    public void addAllPageRanges(Collection<PageRange> ranges) {
        pageSelection.addAll(ranges);
    }

    /**
     * @return true if page selection for this input contains all the pages of the input source.
     */
    public boolean isAllPages() {
        return pageSelection.isEmpty();
    }

    /**
     * @param totalNumberOfPage
     *            the number of pages of the document (upper limit).
     * @return the selected set of pages. Iteration ordering is predictable, it is the order in which elements were inserted into the {@link PageRange} set.
     * @see PagesSelection#getPages(int)
     */
    @Override
    public Set<Integer> getPages(int totalNumberOfPage) {
        Set<Integer> retSet = new NullSafeSet<Integer>();
        if (isAllPages()) {
            for (int i = 1; i <= totalNumberOfPage; i++) {
                retSet.add(i);
            }
        } else {
            for (PageRange range : getPageSelection()) {
                retSet.addAll(range.getPages(totalNumberOfPage));
            }
        }
        return retSet;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(source).append(pageSelection).toString();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(source).append(pageSelection).toHashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof org.sejda.model.input.PdfMergeInput)) {
            return false;
        }
        org.sejda.model.input.PdfMergeInput input = (org.sejda.model.input.PdfMergeInput) other;
        return new EqualsBuilder().append(source, input.getSource()).append(pageSelection, input.pageSelection)
                .isEquals();
    }

}

