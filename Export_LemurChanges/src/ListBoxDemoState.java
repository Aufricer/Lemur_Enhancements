/*
 * $Id$
 *
 * Copyright (c) 2016, Simsilica, LLC
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

package demo;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;

import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.ColorRGBA;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.event.PopupState;
import com.simsilica.lemur.list.SelectionModel;
import com.simsilica.lemur.style.ElementId;


import java.util.Iterator;
import java.util.Set;

/**
 *  A demo of the ListBox element.
 *
 *  @author    Paul Speed
 */
public class ListBoxDemoState extends BaseAppState {

    private Container window;

    /**
     *  A command we'll pass to the label pop-up to let
     *  us know when the user clicks away.
     */
    private CloseCommand closeCommand = new CloseCommand();

    private ListBox listBox;

    // Just to give added items a unique suffix
    private int nextItem = 1;

    public ListBoxDemoState() {
    }

    @Override
    protected void initialize( Application app ) {
        app.getInputManager().addMapping("Listboxscrolldown", new KeyTrigger(KeyInput.KEY_DOWN));
        app.getInputManager().addMapping("Listboxscrollup", new KeyTrigger(KeyInput.KEY_UP));
        app.getInputManager().addMapping("Shift", new KeyTrigger(KeyInput.KEY_LSHIFT));
        app.getInputManager().addMapping("Shift", new KeyTrigger(KeyInput.KEY_RSHIFT));
        app.getInputManager().addListener(Lboxlistener, "Listboxscrollup","Listboxscrolldown","Shift");
    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    protected void onEnable() {

        // We'll wrap the text in a window to make sure the layout is working
        window = new Container();
        window.addChild(new Label("List Box Demo", new ElementId("window.title.label")));

        // Change the style of the listbox horizontal slider a bit
        ElementId id = new ElementId("list").child("slider").child("right").child("button");
        GuiGlobals.getInstance().getStyles().getSelector(id, "glass").set("text",">");
        id = new ElementId("list").child("slider").child("left").child("button");
        GuiGlobals.getInstance().getStyles().getSelector(id, "glass").set("text","<");

        // Create a multiline text field with our document model
        listBox = window.addChild(new ListBox());
        listBox.setVisibleItems(5);


        for( int i = 0; i < 2; i++ ) {
            listBox.getModel().add("Item " + nextItem);
            nextItem++;
        }

        // Add some actions that will manipulate the document model independently
        // of the text field
        Label l1 = new Label("Single column, select and color options");
        l1.setTextHAlignment(HAlignment.Center);
        window.addChild(l1);
        Container buttons = window.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y)));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "add")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "insert")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "remove")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "Selectmode")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "Color")));

        Label l2 = new Label("Multi column options");
        l2.setTextHAlignment(HAlignment.Center);
        window.addChild(l2);

        Container button2 = window.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y)));
        button2.addChild(new ActionButton(new CallMethodAction(this, "addColumn")));
        button2.addChild(new ActionButton(new CallMethodAction(this, "insertColumn")));
        button2.addChild(new ActionButton(new CallMethodAction(this, "removeColumn")));
        button2.addChild(new ActionButton(new CallMethodAction(this, "replace")));
        Container button3 = window.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y)));
        button3.addChild(new ActionButton(new CallMethodAction(this, "incavailableColumns")));
        button3.addChild(new ActionButton(new CallMethodAction(this, "decavailableColumns")));
        Container button4 = window.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y)));
        button4.addChild(new ActionButton(new CallMethodAction(this, "incvisibleColumns")));
        button4.addChild(new ActionButton(new CallMethodAction(this, "decvisibleColumns")));
        Container button5 = window.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y)));
        button5.addChild(new ActionButton(new CallMethodAction(this, "getValue_and_reset_Layout")));


        // Add a close button to both show that the layout is working and
        // also because it's better UX... even if the popup will close if
        // you click outside of it.
        window.addChild(new ActionButton(new CallMethodAction("Close",
                window, "removeFromParent")));

        // Position the window and pop it up
        window.setLocalTranslation(400, 400, 100);
        getState(PopupState.class).showPopup(window, closeCommand);
    }

    @Override
    protected void onDisable() {
        window.removeFromParent();
    }

    protected void add() {
        // Add it to the end
        listBox.getModel().add("Added item " + (nextItem++));
    }

    protected void addColumn() { // MultiColumnadd
        //      listBox.lbaddvalue("Test 1");
        String [] T = new String[ listBox.availableColumns];
        int o = 0;
        int selection = listBox.getModel().size();
        for (String elm: T) {
            T[o] = "AddCol " + (selection+1) +"-" + (o+1);
            o++;
        }
        listBox.lbaddvalue(T);
    }


    protected void insert() {
        int selection=0;
        Set<Integer> selectionset = listBox.getSelectionModel().getMultiSelection();
        if( selectionset == null ) {
            selection = listBox.getModel().size();
        }  else {
            // we insert always after last entry so we don't need to care for the position
            // of the selectors we already have
            Iterator<Integer> itset = selectionset.iterator();
            while (itset.hasNext()) {
                selection = itset.next()+1;
            }
        }
        listBox.getModel().add(selection, "Inserted item " + (nextItem++));
    }

    protected void insertColumn() { // Multicolumninsert
        int selection=0;
        Set<Integer> selectionset = listBox.getSelectionModel().getMultiSelection();
        if( selectionset == null ) {
            selection = listBox.getModel().size();
        }  else {
            // we insert always after last entry so we don't need to care for the position
            // of the selectors we already have
            Iterator<Integer> itset = selectionset.iterator();
            while (itset.hasNext()) {
                selection = itset.next()+1;
            }
        }
        String [] T = new String[ listBox.availableColumns];
        int o = 0;
        for (String elm: T) {
            T[o] = "Insert " + (selection+1) +"-" + (o+1);
            o++;
        }
        listBox.lbaddvalue(selection, T);
    }

    protected void replace() {
        int selection =0;
        int i=0;
        Set<Integer> selectionset = listBox.getSelectionModel().getMultiSelection();
        if( selectionset == null ) {
            return;
        } else {
            Iterator<Integer> itset = selectionset.iterator();
            while (itset.hasNext()) {
                selection = itset.next()-i;
                i++;
            }
            String[] repl = new String[listBox.availableColumns];
            for (i=0;i<listBox.availableColumns;i++) {
                if ((i % 2) == 0) repl[i] = "Replaced " + (selection+1) + "-" + (i+1);
            }
            listBox.lbreplacevalue(selection,repl,true);
        }
    }



    protected void removeColumn() {
        int selection;
        int i=0;
        Set<Integer> selectionset = listBox.getSelectionModel().getMultiSelection();
        if( selectionset == null ) {
            return;
        } else {
            Iterator<Integer> itset = selectionset.iterator();
            while (itset.hasNext()) {
                selection = itset.next()-i;
                listBox.lbremovevalue((int)selection);
                i++;
            }
            listBox.getSelectionModel().clear();
        }
    }

    protected void remove() {
        int selection;
        int i=0;
        Set<Integer> selectionset = listBox.getSelectionModel().getMultiSelection();
        if( selectionset == null ) {
            return;
        } else {
            Iterator<Integer> itset = selectionset.iterator();
            while (itset.hasNext()) {
                selection = itset.next()-i;
                listBox.getModel().remove((int)selection);
                i++;
            }
            listBox.getSelectionModel().clear();
        }
    }

    protected void Selectmode() { // MultiSelection
        if (listBox.getSelectionMode().equals(SelectionModel.SelectionMode.Single)){
            listBox.setSelectionMode(SelectionModel.SelectionMode.Multi);
        } else {
            listBox.setSelectionMode(SelectionModel.SelectionMode.Single);
        }
    }

    protected void getValue_and_reset_Layout() {
        // get the text of first row and readd it
        String[] Test = listBox.getlbvalue(0);
        listBox.lbaddvalue(Test);

        if (listBox.getGridPanel().getColumnwidths() ==null) {
            // Change the layout so we can see different column sizes
            listBox.getGridPanel().setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.ForcedEven, FillMode.None));
            // Set a columnwidth for max 5 columns. Note: width is only set if column exist!
            listBox.getGridPanel().setColumnwidths(new Float[]{120f, 80f, null, 100f, 60f}, true);
            /*
            float temp = 0;
            for (Float f : listBox.getGridPanel().getColumnwidths()) {
                if (f != null) temp += f;
            }
            listBox.setPreferredSize(listBox.getPreferredSize().clone().setX(temp));
            */
            //  listBox.getGridPanel().setHalignements(new HAlignment[]{HAlignment.Right,HAlignment.Right,HAlignment.Right,HAlignment.Right,HAlignment.Right});
            listBox.getGridPanel().setHalignements(HAlignment.Center,0);
        } else {
            listBox.getGridPanel().setLayout(new SpringGridLayout(Axis.Y, Axis.X, FillMode.ForcedEven, FillMode.ForcedEven));
            listBox.setPreferredSize(null);
            listBox.getGridPanel().setColumnwidths(null);
            listBox.getGridPanel().setHalignements(null);
        }
    }

    protected void Color () {
        listBox.setSelectorColor(ColorRGBA.randomColor(),true);
    }


    protected void incvisibleColumns () {
        listBox.setVisibleColumns(listBox.getGridPanel().getVisibleColumns()+1);
    }

    protected void decvisibleColumns () {
        listBox.setVisibleColumns(listBox.getGridPanel().getVisibleColumns()-1);
    }

    protected void incavailableColumns () {
        listBox.setavailableColumns(listBox.availableColumns+1);
    }

    protected void decavailableColumns () {
        listBox.setavailableColumns(listBox.availableColumns-1);
    }

    private class CloseCommand implements Command<Object> {
        public void execute( Object src ) {
            getState(MainMenuState.class).closeChild(ListBoxDemoState.this);
        }
    }


    private boolean shift = false;
    private boolean lastmove;
    private final ActionListener Lboxlistener = new ActionListener() {
        // select lines in lb and move grid if out of visible grid
        @Override
        public void onAction(String name, boolean keyPressed, float tpf) {

            if (name.equals("Shift"))  {
                if (keyPressed) {
                    shift = true;
                } else {
                    shift = false;
                }
            }
            if (name.equals("Listboxscrollup") && !keyPressed) {
                MovemyAss(true);
            }
            if (name.equals("Listboxscrolldown") && !keyPressed) {
                MovemyAss(false);
            }
        }

        private void MovemyAss(boolean UP) {
            int t =0;
            if (!(shift) && listBox.getSelectionMode().equals(SelectionModel.SelectionMode.Single)) {
                t = 1;
                if (UP) t = -1;
                Integer selection;
                if (listBox.getSelectionModel().getSelection() == null) return;
                selection = listBox.getSelectionModel().getSelection();
                if (selection + t > -1) listBox.getSelectionModel().add(selection + t); // no negative values
                selection = listBox.getSelectionModel().getSelection();
                movegridview(selection);
                return;
            }

            if (shift && listBox.getSelectionMode().equals(SelectionModel.SelectionMode.Multi)) {
                if (UP && lastmove) t=-1; // row before checked
                if (!(UP) && (!lastmove)) t = 1; // row after checked
                Integer selection;
                try {
                    selection = listBox.getSelectionModel().getlastAdd()+t;
                    // position = anker;
                }
                catch (NullPointerException e) {
                    return;// (( no lastadd))
                }

                if (listBox.getSelectionModel().contains(selection)) {
                    listBox.getSelectionModel().remove(selection);
                    listBox.getSelectionModel().add(selection); // to set lastadd
                    listBox.getSelectionModel().remove(selection);
                    lastmove = UP;
                } else {
                    if (selection >= listBox.getModel().size() || selection<0) return; // invalid add (end of listbox)
                    listBox.getSelectionModel().add(selection);
                    lastmove = UP;
                }
                movegridview(selection);
                return;
            }
        }
        // listBox.getGridPanel().getRow() --> aktuelle Position im Grid
        // listBox.getGridPanel().getVisibleRows() --> Anzahl der (max.) angezeigten Zeilen im Grid
        // listBox.getGridPanel().setLocation( Zeile, Spalte) --> Bewegung des Grids
        private void movegridview(int newpos){

            if (newpos  - listBox.getGridPanel().getRow() >= listBox.getGridPanel().getVisibleRows() && newpos < listBox.getModel().size()) {
                //      listBox.getGridPanel().setLocation(listBox.getGridPanel().getRow()+1,listBox.getGridPanel().getColumn());
                listBox.getGridPanel().setLocation(newpos - listBox.getGridPanel().getVisibleRows() + 1, listBox.getGridPanel().getColumn());
                return;
            }
            if (newpos < listBox.getGridPanel().getRow()) {
                listBox.getGridPanel().setLocation(Math.max(0,newpos), listBox.getGridPanel().getColumn());
                return;
            }
        }


    };
}



