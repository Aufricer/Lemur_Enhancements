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

import java.util.*;

import org.slf4j.*;

import com.google.common.base.Objects;

import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.*;
import com.jme3.scene.*;

import com.simsilica.lemur.component.*;
import com.simsilica.lemur.core.*;
import com.simsilica.lemur.event.*;
import com.simsilica.lemur.grid.GridModel;
import com.simsilica.lemur.list.*;
import com.simsilica.lemur.style.*;


/**
 *
 *  @author    Paul Speed
 */
public class ListBox<T> extends Panel {

    static Logger log = LoggerFactory.getLogger(ListBox.class);

    public static final String ELEMENT_ID = "list";
    public static final String CONTAINER_ID = "container";
    public static final String ITEMS_ID = "items";
    public static final String SLIDER_ID = "slider";
    public static final String SELECTOR_ID = "selector";

    public static final String EFFECT_PRESS = "press";
    public static final String EFFECT_RELEASE = "release";
    public static final String EFFECT_CLICK = "click";
    public static final String EFFECT_ACTIVATE = "activate";
    public static final String EFFECT_DEACTIVATE = "deactivate";

    public enum ListAction { Down, Up, Click, Entered, Exited };


    private BorderLayout layout;
    private VersionedList<T> model;
    private VersionedReference<List<T>> modelRef;
    private CellRenderer<T> cellRenderer;

    private SelectionModel selection;
    private VersionedReference<Set<Integer>> selectionRef;

    private ClickListener clickListener = new ClickListener();
    private BackgroundListener backgroundListener = new BackgroundListener();
    private CommandMap<ListBox, ListAction> commandMap
            = new CommandMap<ListBox, ListAction>(this);

    private GridPanel grid;
    private Slider slider;
    private Node selectorArea;
    private Panel selector;
    private Vector3f selectorAreaOrigin = new Vector3f();
    private Vector3f selectorAreaSize = new Vector3f();
    private RangedValueModel baseIndex;  // upside down actually
    private VersionedReference<Double> indexRef;
    private int maxIndex;

    private SelectorClickListener selectorlistener = new SelectorClickListener(); // get rid of the selectorpanels via click
    private List<T>[] lbcolumn; // the columns > column 1
    public int availableColumns;
    private static ElementId meid; // Need that for styling
    private boolean tester = false; // need that as I can not change the order of attributes
    private RangedValueModel baseIndexhor;  // upside down actually
    private Slider sliderhor;
    private VersionedReference<Double> indexRefhor;
    /**
     *  Set to true the mouse wheel will scroll the list if the mouse
     *  is over the list.
     */
    private boolean scrollOnHover = true;

    /**
     *  Keeps track of if we've triggered 'activated' effects (and send entered events)
     */
    private boolean activated = false;

    /**
     *  Keeps track of whether some listener has detected enter/exit.  When this
     *  is different than activated then we need to trigger effects and fire events.
     */
    private boolean entered = false;

    public ListBox() {
        this(true, new VersionedList<T>(), null,
                new SelectionModel(),
                new ElementId(ELEMENT_ID), null);
    }

    public ListBox( VersionedList<T> model ) {
        this(true, model, null,
                new SelectionModel(), new ElementId(ELEMENT_ID), null);
    }

    public ListBox( VersionedList<T> model, CellRenderer<T> renderer, String style ) {
        this(true, model, renderer, new SelectionModel(), new ElementId(ELEMENT_ID), style);
    }

    public ListBox( VersionedList<T> model, String style ) {
        this(true, model, null, new SelectionModel(), new ElementId(ELEMENT_ID), style);
    }

    public ListBox( VersionedList<T> model, ElementId elementId, String style ) {
        this(true, model, null, new SelectionModel(), elementId, style);
    }

    public ListBox( VersionedList<T> model, CellRenderer<T> renderer, ElementId elementId, String style ) {
        this(true, model, renderer, new SelectionModel(), elementId, style);
    }

    protected ListBox( boolean applyStyles, VersionedList<T> model, CellRenderer<T> cellRenderer,
                       SelectionModel selection,
                       ElementId elementId, String style ) {
        super(false, elementId.child(CONTAINER_ID), style);

        if( cellRenderer == null ) {
            // Create a default one
            cellRenderer = new DefaultCellRenderer(elementId.child("item"), style);
        }
        this.cellRenderer = cellRenderer;

        this.layout = new BorderLayout();
        getControl(GuiControl.class).setLayout(layout);

        grid = new GridPanel(new GridModelDelegate(), elementId.child(ITEMS_ID), style);
        grid.setVisibleColumns(1);
        grid.getControl(GuiControl.class).addListener(new GridListener());
        layout.addChild(grid, BorderLayout.Position.Center);

        baseIndex = new DefaultRangedValueModel();
        indexRef = baseIndex.createReference();
        slider = new Slider(baseIndex, Axis.Y, elementId.child(SLIDER_ID), style);
        layout.addChild(slider, BorderLayout.Position.East);

        // horizontal slider
        baseIndexhor = new DefaultRangedValueModel();
        sliderhor = new Slider(baseIndexhor, Axis.X, new ElementId("list").child(SLIDER_ID), style);
        layout.addChild(sliderhor, BorderLayout.Position.South);
        indexRefhor = baseIndexhor.createReference();


        if( applyStyles ) {
            Styles styles = GuiGlobals.getInstance().getStyles();
            meid = this.getElementId();
            styles.applyStyles(this, getElementId(), style);
            tester = true;
        }

        // Listen to our own mouse events that don't hit something else
        CursorEventControl.addListenersToSpatial(this, backgroundListener);

        // Need a spacer so that the 'selector' panel doesn't think
        // it's being managed by this panel.
        // Have to set this up after applying styles so that the default
        // styles are properly initialized the first time.
        selectorArea = new Node("selectorArea");
        attachChild(selectorArea);
        selector = new Panel(elementId.child(SELECTOR_ID), style);
        CursorEventControl.addListenersToSpatial(selector, selectorlistener);

        setModel(model);
        resetModelRange();
        setSelectionModel(selection);
    }

    @StyleDefaults(ELEMENT_ID)
    public static void initializeDefaultStyles( Styles styles, Attributes attrs ) {

        ElementId parent = new ElementId(ELEMENT_ID);
        //QuadBackgroundComponent quad = new QuadBackgroundComponent(new ColorRGBA(0.5f, 0.5f, 0.5f, 1));
        QuadBackgroundComponent quad = new QuadBackgroundComponent(new ColorRGBA(0.8f, 0.9f, 0.1f, 1));
        quad.getMaterial().getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Exclusion);
        styles.getSelector(parent.child(SELECTOR_ID), null).set("background", quad, false);
        styles.getSelector(meid, null).set("availableColumns", 1,false);
        styles.getSelector(meid, null).set("visibleColumns", 1,false);


    }

    @Override
    public void updateLogicalState( float tpf ) {
        super.updateLogicalState(tpf);

        if( modelRef.update() ) {
            resetModelRange();
        }

        boolean indexUpdate = indexRef.update();
        boolean selectionUpdate = selectionRef.update();
        boolean indexhorUpdate = indexRefhor.update();
        if( indexUpdate ) {
            int index = (int)(maxIndex - baseIndex.getValue());
            grid.setRow(index);
        }

        if (indexhorUpdate) {
            getGridPanel().setLocation(getGridPanel().getRow(),(int) baseIndexhor.getValue());

        }
        if( selectionUpdate || indexUpdate ) {
            refreshSelector();
        }

        if( activated != entered ) {
            refreshActivation();
        }
    }

    protected void gridResized( Vector3f pos, Vector3f size ) {
        if( pos.equals(selectorAreaOrigin) && size.equals(selectorAreaSize) ) {
            return;
        }

        selectorAreaOrigin.set(pos);
        selectorAreaSize.set(size);

        refreshSelector();
    }

    public void setModel( VersionedList<T> model ) {
        if( this.model == model && model != null ) {
            return;
        }

        if( this.model != null ) {
            // Clean up the old one
            detachItemListeners();
        }

        if( model == null ) {
            // Easier to create a default one than to handle a null model
            // everywhere
            model = new VersionedList<T>();
        }

        this.model = model;
        this.modelRef = model.createReference();

        adjustothercolumnmodel(); // prepare the other columns

        grid.setLocation(0,0);
        grid.setModel(new GridModelDelegate());  // need a new one for a new version
        resetModelRange();
        baseIndex.setValue(maxIndex);
        refreshSelector();
    }

    public VersionedList<T> getModel() {
        return model;
    }

    public Slider getSlider() {
        return slider;
    }

    public GridPanel getGridPanel() {
        return grid;
    }

    public void setSelectionModel( SelectionModel selection ) {
        if( this.selection == selection ) {
            return;
        }
        this.selection = selection;
        this.selectionRef = selection.createReference();
        refreshSelector();
    }

    public SelectionModel getSelectionModel() {
        return selection;
    }

    public void addCommands( ListAction a, Command<? super ListBox>... commands ) {
        commandMap.addCommands(a, commands);
    }

    public List<Command<? super ListBox>> getCommands( ListAction a ) {
        return commandMap.get(a, false);
    }

    public void addClickCommands( Command<? super ListBox>... commands ) {
        commandMap.addCommands(ListAction.Click, commands);
    }

    public void removeClickCommands( Command<? super ListBox>... commands ) {
        getClickCommands().removeAll(Arrays.asList(commands));
    }

    public List<Command<? super ListBox>> getClickCommands() {
        return commandMap.get(ListAction.Click, false);
    }

    @StyleAttribute("listCommands")
    public void setListCommands( Map<ListAction, List<Command<? super ListBox>>> map ) {
        commandMap.clear();
        // We don't use putAll() because (right now) it would potentially
        // put the wrong list implementations into the command map.
        for( Map.Entry<ListAction, List<Command<? super ListBox>>> e : map.entrySet() ) {
            commandMap.addCommands(e.getKey(), e.getValue());
        }
    }

    @StyleAttribute(value="visibleItems", lookupDefault=false)
    public void setVisibleItems( int count ) {
        grid.setVisibleRows(count);
        resetModelRange();
        refreshSelector();
    }


    public int getVisibleItems() {
        return grid.getVisibleRows();
    }

    @StyleAttribute(value="cellRenderer", lookupDefault=false)
    public void setCellRenderer( CellRenderer renderer ) {
        if( Objects.equal(this.cellRenderer, renderer) ) {
            return;
        }
        this.cellRenderer = renderer;
        grid.refreshGrid(); // cheating
    }

    public CellRenderer getCellRenderer() {
        return cellRenderer;
    }

    public void setAlpha( float alpha, boolean recursive ) {
        super.setAlpha(alpha, recursive);

        // Catch some of our intermediaries
        setChildAlpha(selector, alpha);
    }

    /**
     *  Set to true to enable mouse-wheel style scrolling when the
     *  mouse is hovering over the ListBox. (Versus only when the list
     *  has focus.)  Default is true.
     */
    @StyleAttribute(value="scrollOnHover", lookupDefault=false)
    public void setScrollOnHover( boolean f ) {
        this.scrollOnHover = f;
    }

    public boolean getScrollOnHover() {
        return scrollOnHover;
    }

    protected void refreshSelector() {
        if( selectorArea == null ) {
            return;
        }

        // remove current selectors
        selectorArea.detachChildNamed("selectorPanels");
        Panel selectedCell = null;
        if( selection != null && !selection.isEmpty() ) {
            // looping the selected items
            // we always keep one selector to keep code invasion minimal and add additional selectors if we need them

            int selected;
            int max;
            int i = 0;
            int zaehler = 0;
            String tmpstyle = selector.getStyle();
            ElementId tmpElementId = selector.getElementId();

            Node selectorPanels = new Node();
            selectorPanels.setName("selectorPanels");

            // we find the number of selections we need to make visible
            // if single selection mode is activated, we just take one (the first)
            if (getSelectionMode().equals(SelectionModel.SelectionMode.Single)) {
                max = 1;
            } else {
                max = selection.size();
            }


            Iterator<Integer> itsel = selection.iterator();
            while (i < max && itsel.hasNext()) {
                i++;
                selected = itsel.next();
                if (selected >= model.size()) {
                    selected = model.size() - 1;
                    selection.setSelection(selected); // in case we have to many adds we delete and set only the last
                }
                //        selectedCell = grid.getCell(selected, 0); // we always choose the first cell in grid
                selectedCell = grid.getCell(selected, getGridPanel().getColumn()); // we always choose the first cell in gridpanel

                if (selectedCell == null) {
                    // this grid cell is not part of the grid/ out of bounds
                    continue;
                } else {
                    if (zaehler > 0) {
                        // we keep the initial selector from initialization or one of the initial selectors
                        // if we need more then one we "copy" this initial selector
                        // not sure if this is necessary but this way we can make sure
                        // that style and ID are always a derivative from the original selector
                        // + we are following the initial approach in the library

                        // Copy the color and use it otherwise we would get the default color and style
                        ColorRGBA selcol = ((QuadBackgroundComponent) selector.getBackground()).getColor();
                        selector = new Panel(tmpElementId, tmpstyle);
                        ((QuadBackgroundComponent) selector.getBackground()).setColor(selcol);

                        CursorEventControl.addListenersToSpatial(selector, selectorlistener);
                    }

                    // we also add the row number to the selector
                    // in combination with our SelectorClickListener we can easily remove the selector without iterating or adding
                    zaehler++;
                    selector.setUserData("Row", selected);

                    Vector3f size = selectedCell.getSize().clone();
                    Vector3f loc = selectedCell.getLocalTranslation();
                    Vector3f pos = selectorAreaOrigin.add(loc.x, loc.y, loc.z + size.z+2f);
                    selector.setLocalTranslation(pos);

                    // adjust size.x for multicolumn listboxes
                    if (grid.getVisibleColumns()>1) {
                        for (int t = 1; t < grid.getVisibleColumns();t++) {
                            if (!(grid.getCell(selected, t + getGridPanel().getColumn()) == null))      size.addLocal((float) (grid.getCell(selected, t+getGridPanel().getColumn()).getSize().clone().x), 0, 0);
                        }
                    }
                    selector.setSize(size);
                    selector.setPreferredSize(size);

                    selectorPanels.attachChild(selector);
                }
            }
            selectorArea.attachChild(selectorPanels);
            selectorArea.setLocalTranslation(grid.getLocalTranslation());
        }
    }

    protected void resetModelRange() {
        int count = model == null ? 0 : model.size();
        int visible = grid.getVisibleRows();
        maxIndex = Math.max(0, count - visible);

        // Because the slider is upside down, we have to
        // do some math if we want our base not to move as
        // items are added to the list after us
        double val = baseIndex.getMaximum() - baseIndex.getValue();

        baseIndex.setMinimum(0);
        baseIndex.setMaximum(maxIndex);
        baseIndex.setValue(maxIndex - val);
    }

    protected void refreshActivation() {
        if( entered ) {
            activate();
        } else {
            deactivate();
        }
    }

    protected Panel getListCell( int row, int col, Panel existing ) {
        int refsize = model.size();
        T value;
        if (col == 0) {
            value = model.get(row);
        } else  {
            value = (T) (""); // just visual
            // if we dont have multicolumn we dont need to do anything
            if (!(lbcolumn == null) ) {
                if (lbcolumn.length >= col) {
                    // we check and adjust in case we find that the other columns differ in length
                    // what could have been caused by wrong remove
                    if (lbcolumn[col-1].size()> refsize)      lbcolumnadjust();
                    if (lbcolumn[col-1].size()<=row) { // or wrong add
                        lbcolumn[col-1].add((T) ("" ));
                    } else { // we just load the correct value
                        value = lbcolumn[col-1].get(row);
                    }
                }
            }
        }
        Panel cell = cellRenderer.getView(value, false, existing);
        // we add an invisible background to that cell - as the clickListener otherwise would only apply to the
        // parts of the cell where text is shown
        if (cell.getBackground() == null) {
            cell.setBackground(new QuadBackgroundComponent(new ColorRGBA(ColorRGBA.BlackNoAlpha)));
            //      cell.setBackground(new QuadBackgroundComponent(new ColorRGBA(ColorRGBA.randomColor())));
        }

        if( cell != existing ) {
            // Transfer the click listener
            CursorEventControl.addListenersToSpatial(cell, clickListener);
            CursorEventControl.removeListenersFromSpatial(existing, clickListener);
        }
        return cell;
    }

    /**
     *  Used when the list model is swapped out.
     */
    protected void detachItemListeners() {
        int base = grid.getRow();
        int colbase = grid.getColumn();
        for( int i = 0; i < grid.getVisibleRows(); i++ ) {
            for (int j = 0; j< grid.getVisibleColumns();j++){
                Panel cell = grid.getCell(base + i, colbase+j);
                if( cell != null ) {
                    CursorEventControl.removeListenersFromSpatial(cell, clickListener);
                }
            }
        }
    }

    protected void scroll( int amount ) {
        double delta = getSlider().getDelta();
        double value = getSlider().getModel().getValue();
        getSlider().getModel().setValue(value + delta * amount);
    }

    protected void activate() {
        if( activated ) {
            return;
        }
        activated = true;
        commandMap.runCommands(ListAction.Entered);
        runEffect(EFFECT_ACTIVATE);
    }

    protected void deactivate() {
        if( !activated ) {
            return;
        }
        activated = false;
        commandMap.runCommands(ListAction.Exited);
        runEffect(EFFECT_DEACTIVATE);
    }

    @Override
    public String toString() {
        return getClass().getName() + "[elementId=" + getElementId() + "]";
    }

    private class ClickListener extends DefaultCursorListener {

        // tracks whether we've sent entered events or not
        //private boolean entered = false;

        // Tracks whether we've sent pressed events or not
        private boolean pressed = false;

        @Override
        protected void click( CursorButtonEvent event, Spatial target, Spatial capture ) {
            //if( !isEnabled() )
            //    return;
            commandMap.runCommands(ListAction.Click);
            runEffect(EFFECT_CLICK);
        }

        @Override
        public void cursorButtonEvent( CursorButtonEvent event, Spatial target, Spatial capture ) {

            // Find the element we clicked on
            int base = grid.getRow();
            int colbase = grid.getColumn();
            for( int i = 0; i < grid.getVisibleRows(); i++ ) {
                for (int j = 0; j < grid.getVisibleColumns();j++) {
                    //   Panel cell = grid.getCell(base + i, 0);
                    Panel cell = grid.getCell(base + i, j+colbase);
                    if (cell == target) {
                        // we just need to know the clicked line
                        // then we break up the loop
                        selection.add(base + i);
                        i = grid.getVisibleRows();
                        break;
                    }
                }
            }


            // List boxes always consume their click events
            event.setConsumed();

            // Do our own better handling of 'click' now
            //if( !isEnabled() )
            //    return;
            if( event.isPressed() ) {
                pressed = true;
                commandMap.runCommands(ListAction.Down);
                runEffect(EFFECT_PRESS);
            } else {
                if( target == capture ) {
                    // Then we are still over the list box and we should run the
                    // click
                    click(event, target, capture);
                }
                // If we run the up without checking properly then we
                // potentially get up events with no down event.  This messes
                // up listeners that are (correctly) expecting an up for every
                // down and no ups without downs.
                // So, any time the capture is us then we will run, else not.
                // ...but that's not right either because if we consume the
                // event (which we do) then the capture will be the item and not
                // the list.  Not sure how it ever worked like that... but I'm
                // leaving it here commented out just in case.
                //if( capture == ListBox.this ) {
                //    commandMap.runCommands(ListAction.Up);
                //    runEffect(EFFECT_RELEASE);
                //}
                if( pressed ) {
                    commandMap.runCommands(ListAction.Up);
                    runEffect(EFFECT_RELEASE);
                    pressed = false;
                }
            }
        }

        @Override
        public void cursorEntered( CursorMotionEvent event, Spatial target, Spatial capture ) {
            entered = true;
            /*
            Not sure how this code ever worked but it looks like I meant it.  I can
            find no use-cases in my own codebase so I'm not sure what I was thinking that day.
            Leaving it just in case.
            TODO: may need to readdress if we refactor the mouse/cursor events processing.
            if( capture == ListBox.this || (target == ListBox.this && capture == null) ) {
                entered = true;
                commandMap.runCommands(ListAction.Entered);
                runEffect(EFFECT_ACTIVATE);
            }*/
        }

        @Override
        public void cursorExited( CursorMotionEvent event, Spatial target, Spatial capture ) {
            entered = false;
            /*if( entered ) {
                commandMap.runCommands(ListAction.Exited);
                runEffect(EFFECT_DEACTIVATE);
                entered = false;
            }*/
        }
    }

    public void setSelectionMode(SelectionModel.SelectionMode smode) {
        selection.setSelectionMode(smode);
    }

    public SelectionModel.SelectionMode getSelectionMode() {
        return selection.getSelectionMode();
    }

    public void setSelectorColor(ColorRGBA newColor, boolean keepAlpha) {
        float Alpha;
        if (keepAlpha) {
            Alpha = selector.getAlpha();
        } else {
            Alpha = newColor.getAlpha();
        }
        QuadBackgroundComponent selbg = (QuadBackgroundComponent) selector.getBackground();
        selbg.setColor(new ColorRGBA(newColor.r, newColor.g, newColor.b, Alpha));
        refreshSelector();
    }

    /**
     *  Listens to the whole list to intercept things like mouse wheel events
     *  and click to focus.  This should be all we need for hover scrolling as
     *  long as the cell renderers don't consume the motion events.
     */
    private class BackgroundListener extends DefaultCursorListener {

        @Override
        public void cursorEntered( CursorMotionEvent event, Spatial target, Spatial capture ) {
            entered = true;
        }

        @Override
        public void cursorExited( CursorMotionEvent event, Spatial target, Spatial capture ) {
            entered = false;
        }

        @Override
        public void cursorMoved( CursorMotionEvent event, Spatial target, Spatial capture ) {
            if( event.getScrollDelta() != 0 ) {
                if( log.isTraceEnabled() ) {
                    log.trace("Scroll delta:" + event.getScrollDelta() + "  value:" + event.getScrollValue());
                }
                if( scrollOnHover ) {
                    // My wheel moves in multiples of 120... I don't know if that's
                    // universal so we'll at least always send some value.
                    if( event.getScrollDelta() > 0 ) {
                        scroll(Math.max(1, event.getScrollDelta() / 120));
                    } else {
                        scroll(Math.min(-1, event.getScrollDelta() / 120));
                    }
                }
            }
        }
    }

    private class GridListener extends AbstractGuiControlListener {
        public void reshape( GuiControl source, Vector3f pos, Vector3f size ) {
            gridResized(pos, size);

            // If the grid was re-laid out then we probably need
            // to refresh our selector
            refreshSelector();
        }
    }

    protected class GridModelDelegate implements GridModel<Panel> {

        @Override
        public int getRowCount() {
            if( model == null ) {
                return 0;
            }
            return model.size();
        }

        @Override
        public int getColumnCount() {
            //   return grid.getVisibleColumns();
            return availableColumns; // changed as visibleColumns may differ from available columns
        }

        @Override
        public Panel getCell( int row, int col, Panel existing ) {
            return getListCell(row, col, existing);
        }

        @Override
        public void setCell( int row, int col, Panel value ) {
            throw new UnsupportedOperationException("ListModel is read only.");
        }

        @Override
        public long getVersion() {
            return model == null ? 0 : model.getVersion();
        }

        @Override
        public GridModel<Panel> getObject() {
            return this;
        }

        @Override
        public VersionedReference<GridModel<Panel>> createReference() {
            return new VersionedReference<GridModel<Panel>>(this);
        }
    }

    private class SelectorClickListener extends DefaultCursorListener {
        @Override
        protected void click(CursorButtonEvent event, Spatial target, Spatial capture) {
            //  lastuserdeselectedRow = target.getUserData("Row"); maybe later, if needed
            selection.remove(target.getUserData("Row"));
        }

    }


// NEU AB 09.2018


    // @StyleAttribute(value="availableColumns", lookupDefault=false)
    @StyleAttribute(value="availableColumns")
    public void setavailableColumns( int count ) {
        if (availableColumns == count) return;
        // we have always at least 1 column and minimum the number of visible column
        this.availableColumns = Math.max(1,(count));
        if (grid.getVisibleColumns()>this.availableColumns) {
            setVisibleColumns(availableColumns);
        }
        sliderhorsetup();
        adjustothercolumnmodel();
        //     System.out.println("Columns available: " +availableColumns);
    }

    @StyleAttribute(value="visibleColumns")
    public void setVisibleColumns( int count ) {
        if (tester) { // tester = false if the styles are applied. unfortuately the styles are called in wrong order so this is needed
            grid.setVisibleColumns(Math.min(Math.max(availableColumns,1),Math.max(count,1))); // max available columns, min 1
        } else {
            grid.setVisibleColumns(Math.max(count,1));
        }
        sliderhorsetup();
        grid.refreshGrid();
        refreshSelector();
        //  System.out.println("Columns visble: " +grid.getVisibleColumns());
    }


    protected void adjustothercolumnmodel(){
        if (lbcolumn == null) {
            if (availableColumns == 1) return;
            List<T>[] tmp = new List[availableColumns -1];
            for (int i = 0; i< availableColumns -1; i++){
                List<T> list = new ArrayList<T>();
                tmp[i] = list;
            }
            lbcolumn =tmp;
        } else {
            if (availableColumns == 1) {
                lbcolumn =null;
                return;
            }
            if (lbcolumn.length == availableColumns -1) return;
            List<T>[] tmp = new List[availableColumns -1];
            int i =0;
            for (List<T> elm: lbcolumn){
                tmp[i] = elm;
                i++;
                if (i == availableColumns-1) break; // now we have fewer columns
            }
            for (int j = i; j< availableColumns -1; j++){
                // we have extra columns and we already fill the empty rows (even if other columns are not filled yet)
                List<T> list = new ArrayList<T>();
                for (int n =0;n<model.size();n++) {
                    list.add((T) "");
                }
                tmp[j] = list;
            }
            lbcolumn =tmp;
        }
    }

    private void lbcolumnadjust(){
        // reduce the available length of the other columns to the length of the first column
        int ref = model.size();
        for (List<T> elm: lbcolumn){
            int elsize = elm.size();
            for (int i = ref;i<elsize;i++) {
                elm.remove(elm.size()-1);
            }
        }
    }

    private String[] prepareaddValue (String[] stringfield, boolean replacenull) {
        String[] tmpvalue = new String[availableColumns];
        if (stringfield == null) return tmpvalue;
        if (stringfield.length != tmpvalue.length) {
            int i;
            for (i=0; i<stringfield.length;i++) {
                if (i == availableColumns) break;
                if ((replacenull) && (stringfield[i] == null)) {
                    tmpvalue[i] = "";
                } else {
                    tmpvalue[i] = stringfield[i];
                }
            }
            for (int z = i; z<=availableColumns-1;z++) {
                tmpvalue[z] ="";
            }
        } else {
            if (replacenull) {
                for (int i =0;i<stringfield.length;i++){
                    if (stringfield[i] == null) stringfield[i] ="";
                }
            }
            return  stringfield;
        }
        return tmpvalue;
    }

    public void lbaddvalue(String singlecolumn) {
        String[] x = new String[1];
        x[0] = singlecolumn;
        multiadd(model.size(),prepareaddValue(x,false));
    }

    public void lbaddvalue(String[] values) {
        int pos = getModel().size();
        multiadd(pos,prepareaddValue(values,true));
    }

    public void lbaddvalue(String[] values, boolean insertnullvalues) {
        int pos = getModel().size();
        multiadd(pos,prepareaddValue(values,!insertnullvalues));
    }

    public void lbaddvalue(int row,String singlecolumn) {
        if (row > model.size()) return;
        String[] x = new String[1];
        x[0] = singlecolumn;
        multiadd(row,prepareaddValue(x,false));
    }

    public void lbaddvalue(int row,String[] values) {
        if (row > model.size()) return;
        multiadd(row,prepareaddValue(values,true)); // insert at position
    }

    public void lbbaddvalue(int row, String[] values, boolean insertnullvalues) {
        if (row > model.size()) return;
        multiadd(row,prepareaddValue(values,!insertnullvalues)); // insert at position
    }


    private void multiadd(int row, String[] value) {
        getModel().add(row, (T) value[0]);
        for (int i = 1; i < availableColumns;i++) {
            if (i >= value.length) break;
            int z = row - lbcolumn[i-1].size();
            for (int x = 1; x <= z;x++) {
                lbcolumn[i-1].add(lbcolumn[i-1].size(), (T) ""); // add missing rows
            }
            lbcolumn[i-1].add(Math.min(lbcolumn[i-1].size(),row), (T) value[i]);
        }
    }

    /* ToDo delete if this works without problem
    13.10.2018 we change the behaviour of updating the columns > 0
    before we used the grid.refresh function to always have the current state in our grid
    we avoided calling this method to often
    Unfortunately this did not increase the version of our model (in case only columns > 0 refreshed)
    So we could not get an information about the listbox changed from outside
    we are changing this by making the inc version public and increasing it each time we
    change a value of the lb in a column > 0 thus the grid.refresh is called via the update loop
    and only once, no matter how often we increase the model version
    */

    //  private boolean singlerefresh = true; ToDo 13.10.2018 delete
    public void lbreplacevalue(int row, int col, String cellvalue) {
        if (row > model.size()-1) return;
        if (col == 0) {
            model.set(row,(T) cellvalue);
        } else {
            if (lbcolumn == null) return;
            if (lbcolumn.length < col) return;
            if ((lbcolumn[col-1].size() < row) || (lbcolumn[col-1].size() ==0)) {
                int z = row+1 - lbcolumn[col-1].size(); // number of missing rows
                for (int x = 0; x < z;x++) {
                    lbcolumn[col-1].add(lbcolumn[col-1].size(), (T) ""); // add missing rows
                }
            }
            lbcolumn[col-1].set(row,(T) cellvalue);
            model.incrementVersion();

            /* ToDo 13.10.2018 delete
            if (singlerefresh) {
                grid.refreshGrid(); // we refresh the only grid onces
                refreshlbmodel(); // cheating - we increase the version of the modelref
            }
            */
        }
    }

    public void lbreplacevalue(int row, String cellvalue) {
        lbreplacevalue(row,0,cellvalue);
    }

    public void lbreplacevalue(int row, String[] value,boolean ignorenullvaluesinfield) {
        int i =0;
        //    singlerefresh = false; // we dont refresh the grid during calls ToDo 13.10.2018 delete
        for (String x:value) {
            if (((x == null) && (!(ignorenullvaluesinfield))) || (!(x==null)) ) lbreplacevalue(row,i,x);
            i++;
            if (i > availableColumns-1) break; // we ignore values > availablecolumns
        }

        /* ToDo 13.10.2018 delete
        if the version is increased during calls, the grid must be autorefreshed
         */
        //    singlerefresh = true;
        //   grid.refreshGrid();
    }


    public void lbremovevalue(int row) {
        if (row > getModel().size()) return;
        getModel().remove(row);
        if (lbcolumn == null) return;
        for (List<T> elm: lbcolumn){
            elm.remove(row);
        }
    }

    public String[] getlbvalue(int row) {
        String[] tmp = new String[availableColumns];
        if (row>model.size()-1) return null;
        tmp[0] = (String) getModel().get(row);
        int i =1;
        if (!(lbcolumn == null)) {
            for (List<T> elm: lbcolumn) {
                if (elm.size()>row) { // empty rows will be ignored
                    tmp[i] = (String) elm.get(row);
                }
                i++;
            }
        }
        return tmp;
    }

    private void sliderhorsetup(){
        if (availableColumns > getGridPanel().getVisibleColumns()) {
            if (sliderhor.getParent() == null) layout.addChild(sliderhor, BorderLayout.Position.South);
            resetModelhorRange();
        } else {
            if (!(sliderhor.getParent() == null))   this.layout.removeChild(sliderhor); // must be detached from layout
            //     if (!(sliderhor.getParent() == null)) sliderhor.getParent().detachChild(sliderhor);
            baseIndexhor.setValue(0);
        }
    }

    public Slider gethorizontalSlider() {
        return sliderhor;
    }

    private void resetModelhorRange() {
        baseIndexhor.setMaximum(availableColumns-getGridPanel().getVisibleColumns());
    }

}
