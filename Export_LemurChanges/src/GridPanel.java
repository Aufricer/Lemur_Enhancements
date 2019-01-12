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

package com.simsilica.lemur;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.grid.GridModel;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.ElementId;
import com.simsilica.lemur.style.StyleDefaults;
import com.simsilica.lemur.style.Styles;


/**
 *
 *  @author    Paul Speed
 */
public class GridPanel extends Panel {

    public static final String ELEMENT_ID = "grid";

    private GridModel<Panel> model;
    private VersionedReference<GridModel<Panel>> modelRef;
    private SpringGridLayout layout;
    private int visibleRows = 5;
    private int visibleColumns = 5;
    private int row = 0;
    private int column = 0;
    private Float alpha; // for setting to new children
    private Float[] columnwidths = null;
    private boolean widthsupdate = false;
    private HAlignment [] columnHalignement = null;


    public GridPanel( GridModel<Panel> model ) {
        this(true, model, new ElementId(ELEMENT_ID), null);
    }

    public GridPanel( GridModel<Panel> model, String style ) {
        this(true, model, new ElementId(ELEMENT_ID), style);
    }

    public GridPanel( GridModel<Panel> model, ElementId elementId, String style ) {
        this(true, model, elementId, style);
    }

    protected GridPanel( boolean applyStyles, GridModel<Panel> model,
                         ElementId elementId, String style ) {
        super(false, elementId, style);

        this.layout = new SpringGridLayout(Axis.Y, Axis.X,
                FillMode.ForcedEven,
                FillMode.ForcedEven);
        getControl(GuiControl.class).setLayout(layout);

        if( applyStyles ) {
            Styles styles = GuiGlobals.getInstance().getStyles();
            styles.applyStyles(this, elementId.getId(), style);
        }

        setModel(model);
    }
    // change the layout if necessary
    public void setLayout(SpringGridLayout lay) {
        this.layout = lay;
        getControl(GuiControl.class).getLayout().clearChildren();
        getControl(GuiControl.class).setLayout(layout);

        this.modelRef = null;
        if( this.model != null ) {
            this.modelRef = model.createReference();
            refreshGrid();
        }

    }


    @StyleDefaults(ELEMENT_ID)
    public static void initializeDefaultStyles( Attributes attrs ) {
    }

    public void setModel( GridModel<Panel> model ) {

        if( this.model == model ) {
            return;
        }

        if( this.model != null ) {
            // Clear the old panel
            getControl(GuiControl.class).getLayout().clearChildren();
            this.modelRef = null;
        }

        this.model = model;

        if( this.model != null ) {
            this.modelRef = model.createReference();
            refreshGrid();
        }
    }

    public GridModel<Panel> getModel() {
        return model;
    }

    public void setRow( int row ) {
        setLocation(row, column);
    }

    public int getRow() {
        return row;
    }

    public void setColumn( int column ) {
        setLocation(row, column);
    }

    public int getColumn() {
        return column;
    }

    public Panel getCell( int r, int c ) {
        r = r - row;
        c = c - column;
        if( r < 0 || c < 0 || r >= visibleRows || c >= visibleColumns ) {
            return null;
        }
        return (Panel)layout.getChild(r, c);
    }

    public void setLocation( int row, int column ) {
        if( this.row == row && this.column == column ) {
            return;
        }
        this.row = row;
        this.column = column;
        refreshGrid();
    }

    public void setVisibleSize( int rows, int columns ) {
        this.visibleRows = rows;
        this.visibleColumns = columns;
        getControl(GuiControl.class).getLayout().clearChildren();
        refreshGrid();
    }

    public void setVisibleRows( int rows ) {
        setVisibleSize(rows, visibleColumns);
    }

    public int getVisibleRows() {
        return visibleRows;
    }

    public void setVisibleColumns( int columns ) {
        setVisibleSize(visibleRows, columns);
    }

    public int getVisibleColumns() {
        return visibleColumns;
    }

    public void setAlpha( float alpha, boolean recursive ) {
        this.alpha = alpha;
        super.setAlpha(alpha, recursive);
    }

    protected void refreshGrid() {
        widthsupdate = false;

        if( model == null ) {
            getControl(GuiControl.class).getLayout().clearChildren();
            return;
        }

        for( int r = row; r < row + visibleRows; r++ ) {
            for( int c = column; c < column + visibleColumns; c++ ) {
                Node existing = layout.getChild(r-row, c-column);
                if( r < 0 || r >= model.getRowCount() || c < 0 || c >= model.getColumnCount() ) {
                    // Out of bounds
                    layout.addChild(null, r-row, c-column);
                } else {
                    Panel child = model.getCell(r, c, (Panel)existing);

                    if (columnwidths != null) {
                        if (columnwidths.length>c) {
                            if (columnwidths[c] !=null)  {
                                child.setPreferredSize(child.getPreferredSize().clone().setX(columnwidths[c]));
                            } else child.setPreferredSize(null);
                        } else child.setPreferredSize(null);
                    }

                    if (columnHalignement != null) {
                        if (columnHalignement.length>c) {
                            if (columnHalignement[c] !=null)  {
                                ((Button) child).setTextHAlignment(columnHalignement[c]);
                            } else ((Button) child).setTextHAlignment(HAlignment.Left);
                        } else ((Button) child).setTextHAlignment(HAlignment.Left);
                    }




                    if( child != existing ) {
                        // Make sure new children pick up the alpha of the container
                        if( alpha != null && alpha != 1 ) {
                            child.setAlpha(alpha);
                        }

                        layout.addChild(child, r-row, c-column);
                    }
                }
            }
        }
    }

    @Override
    public void updateLogicalState( float tpf ) {
        super.updateLogicalState(tpf);

        if( modelRef.update() ) {
            refreshGrid();
        }

        if (widthsupdate)  refreshGrid();

    }

    @Override
    public String toString() {
        return getClass().getName() + "[elementId=" + getElementId() + "]";
    }


    public void setColumnwidths(Float[] widths) {
        setColumnwidths(widths,false);
    }

    public void setColumnwidths(Float[] widths, boolean overrideempty) {
        if (checkexistingwidth(widths)) {
            int i =0;
            for (Float z:widths) {
                setColumnwidthschecked(z,i,overrideempty);
                i++;
                if (i>=columnwidths.length)   return; // we ignore given columnwidth that is out of bound
            }
            if (!overrideempty ) return;
            for (int u = i; u<columnwidths.length;u++) {
                setColumnwidthschecked(null,u,overrideempty); // override existing widths
            }
        }
    }

    public void setColumnwidths(float width, int column) {
        checkexistingwidth(new Float[]{0f});
        if (column < columnwidths.length)  setColumnwidthschecked(width,column,true);
    }

    private void setColumnwidthschecked(Float width, int column,boolean override) {
        if (override || (width !=null))  {
            columnwidths[column] = width;
            widthsupdate = true;
        }
    }

    private boolean checkexistingwidth(Float[] givenfield) {
        // delete the columnwitdhs
        if (givenfield == null) {
            columnwidths =null;
            for (Spatial p: this.getChildren()) {
                Panel x = (Panel) p;
                x.setPreferredSize(null);
            }
            widthsupdate = true;
            return false;
        }
        // or prepare if we have no columnwidths yet
        Float[] tmp;
        if (columnwidths == null) {
            tmp = new Float[model.getColumnCount()];
            columnwidths = tmp;
        } else {
            if (!(columnwidths.length ==model.getColumnCount() )) {
                tmp = new Float[model.getColumnCount()];
                int i = 0;
                for (Float z:columnwidths) {
                    tmp[i] =z;
                    i++;
                    if (i>=model.getColumnCount()) break;
                }
                columnwidths = tmp;
            }
        }
        return true;
    }

    public Float[] getColumnwidths() {
        return columnwidths;
    }



    public void setHalignements(HAlignment[] hals) {
        setHalignements(hals,false);
    }

    public void setHalignements(HAlignment[] hals, boolean overridestandard) {
        if (checkexistinghal(hals)) {
            int i =0;
            for (HAlignment z:hals) {
                setHalignementchecked(z,i,overridestandard);
                i++;
                if (i>=columnHalignement.length)   return; // we ignore given HAlignement that is out of column bound
            }
            if (!overridestandard ) return;
            for (int u = i; u<columnHalignement.length;u++) {
                setHalignementchecked(null,u,overridestandard); // override existing HAlignements
            }
        }
    }

    public void setHalignements(HAlignment hal, int column) {
        checkexistinghal(new HAlignment[]{HAlignment.Center});
        if (column < columnHalignement.length)  setHalignementchecked(hal,column,true);
    }

    private void setHalignementchecked(HAlignment hal, int column,boolean override) {
        if (override || (hal !=null))  {
            columnHalignement[column] = hal;
            widthsupdate = true;
        }
    }

    private boolean checkexistinghal(HAlignment[] givenfield) {
        // delete the given Halignements
        if (givenfield == null) {
            columnHalignement =null;
            for (Spatial p: this.getChildren()) {
                Button x = (Button)((Panel) p);
                x.setTextHAlignment(HAlignment.Left); //standard HAlignement
            }
            widthsupdate = true;
            return false;
        }
        // or prepare if we have no columnHalignement yet
        HAlignment[] tmp;
        if (columnHalignement == null) {
            tmp = new HAlignment[model.getColumnCount()];
            columnHalignement = tmp;
        } else {
            if (!(columnHalignement.length ==model.getColumnCount() )) {
                tmp = new HAlignment[model.getColumnCount()];
                int i = 0;
                for (HAlignment z:columnHalignement) {
                    tmp[i] =z;
                    i++;
                    if (i>=model.getColumnCount()) break;
                }
                columnHalignement = tmp;
            }
        }
        return true;
    }

    public HAlignment[] getColumnHalignement() {
        return columnHalignement;
    }
}
