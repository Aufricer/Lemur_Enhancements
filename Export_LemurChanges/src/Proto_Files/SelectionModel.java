/*
 * $Id$
 *
 * Copyright (c) 2014, Simsilica, LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package Proto_Files;

import com.simsilica.lemur.core.VersionedHolder;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.core.VersionedSet;

import java.util.Objects;
import java.util.Set;

/**
 *
 *  @author    Paul Speed
 */
public class SelectionModel extends VersionedSet<Integer> {

    public enum SelectionMode { Single, Contiguous, Multi }

    private SelectionMode mode = SelectionMode.Single;
    private Integer lastAdd;

    // In single selection, this will contain the current selection.
    private VersionedHolder<Integer> selected = new VersionedHolder<>(null);

    public SelectionModel() {
    }

    /**
     *  For single-selections, this will hold the currently selected value.
     */
    public VersionedReference<Integer> createSelectionReference() {
        return selected.createReference();
    }

    public void setSelectionMode( SelectionMode mode ) {
        if( this.mode == mode ) {
            return;
        }
        if( mode == SelectionMode.Contiguous ) {
            throw new UnsupportedOperationException( "Contiguous selection mode not yet implemented." );
        }
        this.mode = mode;
        if( mode == SelectionMode.Single ) {
            if( size() > 1 ) {
                // Need to clamp it to one... first we'll try the
                // most recent
                if( contains(lastAdd) ) {
                    clear();
                    add(lastAdd);
                } else {
                    // Just grab the first one then
                    Integer temp = iterator().next();
                    clear();
                    add(temp);
                }
            }
        }
        if( mode == SelectionMode.Multi ) {
            // we clear the current selection
            // even if this would not really be necessary
            clear();
        }

    }

    public SelectionMode getSelectionMode() {
        return mode;
    }

    // returns all selected items or null if there is no

    public Set<Integer> getMultiSelection() {
        if (size() == 0) {
            return null;
        }
        return getObject();
    }

    /**
     *  Returns the single selection if there is only one
     *  selected item or null if there is no selection or
     *  more than one item is selected.
     */
    public Integer getSelection() {
        if( size() != 1 ) {
            return null;
        }
        // The only selection should be the last item added
        return lastAdd;
    }

    public int getlastAdd() {
        return lastAdd;
    }

    /**
     *  Sets the currently selected single value regardless
     *  of selection mode.  This clears the set before adding
     *  the selection.  Also, if the specified selection is less
     *  than 0 then the selection is simply cleared.
     */
    public void setSelection( Integer selection ) {
        if( Objects.equals(selection, lastAdd) && size() == 1 )
            return;
        clear();
        if( selection >= 0 ) {
            add(selection);
        }
    }

    @Override
    public boolean add( Integer selection ) {
        if (mode == SelectionMode.Single) {
            if (Objects.equals(selection, lastAdd) && size() == 1)
                return false;
            clear();
            lastAdd = selection;
            return super.add(selection);
        }

        if (mode == SelectionMode.Multi) {
            // delete is done in its own specific ---, no need to search iterate etc. here
            super.add(selection);
            lastAdd = selection;
            return false;
        }
        return false;
    }
}
