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

import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.*;
import com.jme3.scene.*;
import com.simsilica.lemur.component.*;
import com.simsilica.lemur.core.*;
import com.simsilica.lemur.event.*;
import com.simsilica.lemur.grid.GridModel;
import com.simsilica.lemur.list.*;
import com.simsilica.lemur.style.*;
import org.slf4j.*;

import java.util.*;


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

    private ElementId baseElementId;
    private BorderLayout layout;
    private VersionedList<T> model;
    private VersionedReference<List<T>> modelRef;
    private ValueRenderer<T> cellRenderer;

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
    private boolean tester = false; // need that as I can not change the order of attributes
    private RangedValueModel baseIndexhor;  // upside down actually
    private Slider sliderhor;
    private VersionedReference<Double> indexRefhor;

    /* Possible for later - get the last  row deselected by user
    public int getLastuserdeselectedRow() {
        return lastuserdeselectedRow;
    }

    private int lastuserdeselectedRow;
    /*


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

    public ListBox(VersionedList<T> model ) {
        this(true, model, null,
                new SelectionModel(), new ElementId(ELEMENT_ID), null);
    }

    public ListBox(VersionedList<T> model, ValueRenderer<T> renderer, String style ) {
        this(true, model, renderer, new SelectionModel(), new ElementId(ELEMENT_ID), style);
    }

    public ListBox(VersionedList<T> model, String style ) {
        this(true, model, null, new SelectionModel(), new ElementId(ELEMENT_ID), style);
    }

    public ListBox(VersionedList<T> model, ElementId elementId, String style ) {
        this(true, model, null, new SelectionModel(), elementId, style);
    }

    public ListBox(VersionedList<T> model, ValueRenderer<T> renderer, ElementId elementId, String style ) {
        this(true, model, renderer, new SelectionModel(), elementId, style);
    }

    protected ListBox(boolean applyStyles, VersionedList<T> model, ValueRenderer<T> cellRenderer,
                      SelectionModel selection,
                      ElementId elementId, String style ) {

        super(false, elementId.child(CONTAINER_ID), style); //Panel of the ListBox
        this.baseElementId = elementId;

        if( cellRenderer == null ) {
            // Create a default one
            cellRenderer = new DefaultCellRenderer<>(baseElementId.child("item"), style);
        } else {
            cellRenderer.configureStyle(baseElementId.child("item"), style);
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

    @StyleDefaults(ELEMENT_ID) // that very name is used to get the attrs
    public static void initializeDefaultStyles( Styles styles, Attributes attrs ) {
        // OK I dont realy understand how. But in  styles.applyStyles(this, getElementId(), style);
        // those attributes are set but by setting and applying in  public Attributes getSelector( ElementId id, String style )
        // I can see container.list as well as panel when calling.
        // Anyhow here we add currently several more style attributes, that are later called
        // first is for the selector of our listbox
        // and then we need to put style settings to container.list to make that clear
        // I have just introduced customattrs for that

        ElementId parent = new ElementId(ELEMENT_ID);

        QuadBackgroundComponent quad = new QuadBackgroundComponent(new ColorRGBA(0.8f, 0.9f, 0.1f, 1));
        quad.getMaterial().getMaterial().getAdditionalRenderState().setBlendMode(BlendMode.Exclusion);
        // we abuse this method and add something to the selector
        // at this position if style is set null it will be applied to the current style of the ListBox -> all LB will have it
        // otherwise it would only apply for the set style
        styles.getSelector(parent.child(SELECTOR_ID), null).set("background", quad, false);
        // I have honestly no idea why I have to call attributes again
        // as above style = null -> applied to all Listboxes
        Attributes customattrs = styles.getSelector(parent.child(CONTAINER_ID),null); //--> same as baseElementID
        customattrs.set("availableColumns", 1,false);
        customattrs.set("visibleColumns", 1,false);
        customattrs.set("scrollOnHover",true,false);
    //    styles.getSelector(meid, "glass").set("availableColumns", 10,true);

    }

    @Override
    public void updateLogicalState( float tpf ) {
        super.updateLogicalState(tpf);

        if( modelRef.update() ) {
            resetModelRange();
            boolean tmpcheck = false;
            if ((availableColumns>1) && (grid.getVisibleColumns()<availableColumns) && !(lbcolumn == null)) {
                for (int i = grid.getVisibleColumns()-1; i<lbcolumn.length;i++){
                     while (lbcolumn[i].size() < model.size() ) {
                         lbcolumn[i].add((T) "");
                     }
                     if (model.size() > lbcolumn[i].size()) tmpcheck = true;
                }
                if (tmpcheck) lbcolumnadjust();
            }
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
        // in Listbox the Gridpanel only consist of the visible rows and columns
        // if from 6 columns only 5 are visible the grid will only have 5 columns
        // and only those (currently visible) can be modified or the underlying elements (Buttons) can be "extracted"
        // other cells can not be extracted via getGridPanel().getCell() unless they have be loaded to the current layout (into the grid)
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

    /**
     *  Returns the currently selected list item if there is one and only
     *  one item selected.  This is a convenience method that interrogates
     *  the selection model and looks up the current value in the list model.
     */

    public T getSelectedItem() {
        Integer i = selection.getSelection();
        if( i == null ) {
            return null;
        }
        if( i < 0 || i > getModel().size() ) {
            return null;
        }
        return getModel().get(i);
    }

    @SuppressWarnings("unchecked") // because Java doesn't like var-arg generics
    public void addCommands( ListAction a, Command<? super ListBox>... commands ) {
        commandMap.addCommands(a, commands);
    }

    public List<Command<? super ListBox>> getCommands( ListAction a ) {
        return commandMap.get(a, false);
    }

    @SuppressWarnings("unchecked") // because Java doesn't like var-arg generics
    public void addClickCommands( Command<? super ListBox>... commands ) {
        commandMap.addCommands(ListAction.Click, commands);
    }

    @SuppressWarnings("unchecked") // because Java doesn't like var-arg generics
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
    public void setCellRenderer( ValueRenderer<T> renderer ) {
        if( Objects.equal(this.cellRenderer, renderer) ) {
            return;
        }
        this.cellRenderer = renderer;
        // We send through the same element ID that was provided to our constructor
        // because that's what the default cell renderer would have used.
        cellRenderer.configureStyle(baseElementId.child("item"), getStyle());
        grid.refreshGrid(); // cheating
    }

    public ValueRenderer<T> getCellRenderer() {
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
       //           cell.setBackground(new QuadBackgroundComponent(new ColorRGBA(ColorRGBA.randomColor())));
        }

        if( cell != existing ) {
            // Transfer the click listener
            CursorEventControl.addListenersToSpatial(cell, clickListener);
            CursorEventControl.removeListenersFromSpatial(existing, clickListener);
        }
        return cell;
    }


    private void check_columns(int columntocheck){

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
                // Adding a target == ListBox.this check because the capture
                // seems to be the button but the target on release is the list
                // for some reason.
                if( target == capture || target == ListBox.this ) {
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



        /* Notes from 05/2022
        In 2018 I did not realize the potential @pspeed put into Listbox by  using VersionedList<T> as model
        In theory <T> could be a Map<row, column, value> or similiar. -> a multi column list holding any value
        we want. With an custom CellRenderer we could thus probably reach the same as with this modification of Listbox
        Another option would be to modify or extent the VersionedList in a similar way. That would solve the issues we are
        encountering with the choosen approach but would need another change to the constructor in Listbox

        However: WARNING WARNING WARNING WARNING WARNING
        The current modification of Listbox - tried to not change the original Listbox to much
        Whenever multicolumn is in use we internally create a list that's holding lists with the value of listbox
        One list for each column and each of that lists hold all rows of that column (<T> values)
        In that case whenever the original operations on model (model.add, model.remove) etc. are used they only apply
        to the first column. From 2018 on there was some checks that try to ensure that all columns have the same number of rows.
        There is no way for the other columns to know the position of change in the first column, if the model oprions are used
        so ToDo using the model.* functionality in a multicolumn listbox will lead to disorder in the listbox rows
        always use the multi column operations (add_StringValue,  add_Values, replace_LB_value etc.) that ensure a correct behaviour
        The choosen construct makes it necessary to call model.incrementVersion(); in some cases so the grid,.refresh
        functionality is used that (also) ensures that all columns are synchronized
     */


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

    // Column Operations

    protected void adjustothercolumnmodel(){
        if (lbcolumn == null) {
            // if we have only 1 column we use listbox.model
            // otherwise we create a list that holds a list for each column added
            if (availableColumns <=0) setavailableColumns(1); // can only happen due to failed styling
            if (availableColumns == 1) return;
            List<T>[] tmp = new List[availableColumns -1];
            for (int i = 0; i< availableColumns -1; i++){
                List<T> list = new ArrayList<T>();
                for (int n =0;n<model.size();n++) {
                    list.add((T) ""); // to each of that new lists we add empty value for each row
                }
                tmp[i] = list;
            }
            lbcolumn =tmp;
        } else {
            if (availableColumns == 1) {
                lbcolumn =null; //delets all entries in columns > 1 and the lists holding them
                return;
            }
            if (lbcolumn.length == availableColumns -1) return;
            // we make a temporary copy of all LB values and copy those of available columns
            List<T>[] tmp = new List[availableColumns -1];
            int i =0;
            for (List<T> elm: lbcolumn){
                tmp[i] = elm;
                i++;
                if (i == availableColumns-1) break; // in that case we now have fewer columns
            }
            // else we now have extra columns and we already fill the empty rows with empty values
            for (int j = i; j< availableColumns -1; j++){
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

    // Value operations

    public void add_StringValue(int row, int column, String stringValue){
        //adds a String value to the Listbox at the given position
        // all other columns will be filled with ""
        if (row > model.size()) return;
        if (column>availableColumns) return;
        String[] x = new String[availableColumns];
        x[column] = stringValue;
   //     multi_Stringadd(row,prepare_Stringvalue_foradd(x,true));
        multi_Valueadd(row,   prepare_Values_foradd((List<T>) Arrays.asList(x),true));
    }

    public void add_StringValue(String value_column_01){
        // adds a string to the first column of the listbox
        add_StringValue(model.size(),0,value_column_01);
    }

    public void add_StringValue(int row,  String value_column_01) {
        // add or insert a string to the first column at the given row
        add_StringValue(row,0,value_column_01);
    }

    public void add_StringValue(String[] values) {
        //adds the given Values in String as new Line. Non present columns will be replaced by ""
        add_StringValue(getModel().size(),values);
    }

    public void add_StringValue(int row,String[] values) {
        if (row > model.size()) return;
        multi_Valueadd(row,   prepare_Values_foradd((List<T>) Arrays.asList(values),true));
   //     multi_Stringadd(row, prepare_Stringvalue_foradd(values,true)); // insert at position
    }

    public void add_StringValue(Integer row_or_null_for_endoflist, String[] values, boolean replace_nulls) {
        // adds String values to a given position (or at the end) and optional replaces null values with ""
        int pos = getModel().size();
        if (!(row_or_null_for_endoflist == null)) pos = row_or_null_for_endoflist;
        if (pos > model.size()) return;
        multi_Valueadd(pos,prepare_Values_foradd((List<T>) Arrays.asList(values),replace_nulls));
    }

    public void add_Values(int row, int column, T value){
        //adds a value at the given position (row and column) the other columns in row will be filled with ""
        add_StringValue( row,  column,  "replace_me");
        replace_LB_value(row,column,value);
    }

    public void add_Values(Integer row_or_null_for_endoflist, List<T> values){
        //adds a row of Values to the given (or last) line of the Listbox
        int pos = getModel().size();
        if (!(row_or_null_for_endoflist == null)) pos = row_or_null_for_endoflist;
        multi_Valueadd(pos, prepare_Values_foradd(values,true));
    }

    private List<T> prepare_Values_foradd(List<T> unpreparedValues, boolean replacenull) {
        List<T> preparedValues = new ArrayList<T>();
        if (unpreparedValues == null) return preparedValues;
        for (int i = 0; i<availableColumns; i++) {
            if (i < unpreparedValues.size()) {
                preparedValues.add(unpreparedValues.get(i)); // we get what we have from the original list
            } else {
                preparedValues.add(null); // we fill up until we have all columns filled
            }
        }
        if (replacenull) {
            int u = 0;
            for (T listmember: preparedValues) {
                if (listmember == null) {
                    preparedValues.set(u,(T) ""); // replace all null
                }
                u++;
            }
        }
        return preparedValues;
    }

    private void multi_Valueadd(int row, List<T> value) {
        getModel().add(row, (T) value.get(0)); // first column always goes to the default model
        for (int i = 1; i<availableColumns;i++) {
            if (i>= value.size()) break;
            // initially just the rows missing until the row to be added had been added
            // that can happen if model.add is called while some columns are invisible
            // adding an empty row here might lead to a wrong position of values in the "invisible" column
            int z = model.size() - lbcolumn[i-1].size();
            for (int x = 1; x<z;x++) {
                lbcolumn[i-1].add((T) "");
            }
            // each column has model.size()-1 length now and we can add our value at position
            lbcolumn[i-1].add(Math.min(lbcolumn[i-1].size(),row), (T) value.get(i));
        }
    }

    // Replace operations

    public void replace_LB_value(int row, int col, T cellvalue) {
        // replaces the given cells value of listbox (if available but not necessary visible at time)
        // with the given value -> e.g. a Button or a Text
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
            model.incrementVersion(); // updates grid in next update loop
        }
    }

    public void replace_LB_StringValues(int row, String[] value,boolean dontreplace_nulls) {
        // convenient method to replace a whole row in listbox with given Strings
        // starts at column 0 and replaces until all columns in line are replaced or String ends
        // if value contains "null" and those are not replaced, that value will not be replaced in the end
        int i =0;
        for (String x:value) {
            if (((x == null) && !(dontreplace_nulls)) || (!(x==null)) ) replace_LB_value(row,i,(T) x);
            i++;
            if (i > availableColumns-1) break; // we ignore values > availablecolumns
        }
    }

    public void remove_Row(int row) {
        if (row > getModel().size()) return;
        getModel().remove(row);
        if (lbcolumn == null) return;
        for (List<T> elm: lbcolumn){
            if (elm.size()>=1)   elm.remove(row); // remove if there is already a value added
        }
    }

    public List<T> getLBvalues(int row) {
        // returns all values in a ListBox row in its raw form
        List<T> tmp =  new ArrayList<T>();
        if (row>model.size()-1) return null;
        tmp.add(0, (T) getModel().get(row));
        int i =1;
        if (!(lbcolumn == null)) {
            for (List<T> elm: lbcolumn) {
                if (elm.size()>row) { // empty rows will be ignored
                    tmp.add(i, (T) elm.get(row));
                }
                i++;
            }
        }
        return tmp;
    }

    public String[] getlbvalue(int row) {
        // returns all values from a listbox row after converting them to String
        List<T> lBvalues = getLBvalues(row);
        String[] tmp = new String[availableColumns];
        for (int i=0;i<lBvalues.size();i++){
            tmp[i] = lBvalues.get(i).toString();
        }
        return tmp;

        // ToDo 05/2022 -> delete if it works
       /*
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
        /*
        */
    }

    // Horizontal slider operations

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



    // ToDo 05/2022 delete the depreciated functions
    @Deprecated
    public void lbaddvalue(String singlecolumn) {
        String[] x = new String[1];
        x[0] = singlecolumn;
        multi_Stringadd(model.size(), prepare_Stringvalue_foradd(x,false));
    }
    @Deprecated
    public void lbaddvalue(String[] values) {
        int pos = getModel().size();
        multi_Stringadd(pos, prepare_Stringvalue_foradd(values,true));
    }
    @Deprecated
    public void lbaddvalue(String[] values, boolean insertnullvalues) {
        int pos = getModel().size();
        multi_Stringadd(pos, prepare_Stringvalue_foradd(values,!insertnullvalues));
    }
    @Deprecated
    public void lbaddvalue(int row,String singlecolumn) {
        if (row > model.size()) return;
        String[] x = new String[1];
        x[0] = singlecolumn;
        multi_Stringadd(row, prepare_Stringvalue_foradd(x,false));
    }
    @Deprecated
    public void lbaddvalue(int row,String[] values) {
        if (row > model.size()) return;
        multi_Stringadd(row, prepare_Stringvalue_foradd(values,true)); // insert at position
    }
    @Deprecated
    public void lbbaddvalue(int row, String[] values, boolean insertnullvalues) {
        if (row > model.size()) return;
        multi_Stringadd(row, prepare_Stringvalue_foradd(values,!insertnullvalues)); // insert at position
    }
    @Deprecated
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

        }
    }
    @Deprecated
    public void lbreplacevalue(int row, String cellvalue) {
        lbreplacevalue(row,0,cellvalue);
    }
    @Deprecated
    public void lbreplacevalue(int row, String[] value,boolean ignorenullvaluesinfield) {
        int i =0;
        for (String x:value) {
            if (((x == null) && (!(ignorenullvaluesinfield))) || (!(x==null)) ) lbreplacevalue(row,i,x);
            i++;
            if (i > availableColumns-1) break; // we ignore values > availablecolumns
        }
    }
    @Deprecated
    public void lbremovevalue(int row) {
        if (row > getModel().size()) return;
        getModel().remove(row);
        if (lbcolumn == null) return;
        for (List<T> elm: lbcolumn){
            elm.remove(row);
        }
    }
    @Deprecated
    private String[] prepare_Stringvalue_foradd(String[] stringfield, boolean replacenull) {
        // returns a String[] with the number of avasilable columns
        // null values might be replaced with ""
        String[] tmpvalue = new String[availableColumns];
        if (stringfield == null) return tmpvalue;
        if (stringfield.length != tmpvalue.length) {
            int i;
            for (i=0; i<stringfield.length;i++) {
                if (i == availableColumns) break;
                if ((replacenull) && (stringfield[i] == null)) {
                    tmpvalue[i] = "";
                } else {
                    tmpvalue[i] = stringfield[i]; //maybe null is set
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
    @Deprecated
    private void multi_Stringadd(int row, String[] value) {
        getModel().add(row, (T) value[0]); // first column is always the default Listbox model
        for (int i = 1; i < availableColumns;i++) {
            if (i >= value.length) break;
            int z = row - lbcolumn[i-1].size();
            for (int x = 1; x <= z;x++) {
                lbcolumn[i-1].add(lbcolumn[i-1].size(), (T) ""); // add missing rows
            }
            lbcolumn[i-1].add(Math.min(lbcolumn[i-1].size(),row), (T) value[i]);
        }
    }

}
