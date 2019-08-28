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

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.event.*;
import com.simsilica.lemur.style.ElementId;
import com.simsilica.lemur.text.DocumentModel;


import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;


/**
 *  A demo of a Textfield that allows direct entry as well as provides
 *  some buttons for manipulating the DocumentModel separately.
 *
 *  @author    Paul Speed
 */
public class TextEntryDemoState extends BaseAppState {

    private Container window;

    /**
     *  A command we'll pass to the label pop-up to let
     *  us know when the user clicks away.
     */
    private CloseCommand closeCommand = new CloseCommand();

    private TextField textField;
    private DocumentModel document;

    //   private Label txtLabel;

    public TextEntryDemoState() {
    }


    @Override
    protected void initialize( Application app ) {

    }

    @Override
    protected void cleanup( Application app ) {
    }

    @Override
    protected void onEnable() {

        // We'll wrap the text in a window to make sure the layout is working
        window = new Container();
        window.addChild(new Label("Beweglicher Text", new ElementId("window.title.label")));


        // Create a multiline text field with our document model
        // its scrollmode is None and textselect is unstatic by default
        textField = window.addChild(new TextField("Initial text."));

        document = textField.getDocumentModel();

        // Setup some preferred sizing since this will be the primary
        // element in our GUI
        textField.setPreferredWidth(500);
        textField.setPreferredLineCount(10); //space for lines in textfield
        textField.setmaxLinecount(8);        // lines that are used in textfield
        textField.setSingleLine(false);

        // Add some actions that will manipulate the document model independently
        // of the text field
        Container buttons = window.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y)));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "home")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "end")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "forward")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "back")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "insert")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "delete")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "OffsetY")));


        Container button2 = window.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y)));
        button2.addChild(new ActionButton(new CallMethodAction(this, "ScrollModes")));
        button2.addChild(new ActionButton(new CallMethodAction(this, "SelectMode")));
        button2.addChild(new ActionButton(new CallMethodAction(this, "adjust_and_addAreas")));
        button2.addChild(new ActionButton(new CallMethodAction(this, "Color")));
        button2.addChild(new ActionButton(new CallMethodAction(this, "readonly")));



        // Add a close button to both show that the layout is working and
        // also because it's better UX... even if the popup will close if
        // you click outside of it.
        //window.addChild(new ActionButton(new CallMethodAction("Close",
        //                                                      window, "removeFromParent")));
        // Creating a modified close button to more easily test that we really
        // do lose focus and don't keep accepting 'space' to click this button.
        window.addChild(new ActionButton(new CallMethodAction(this, "close")));


        // Position the window and pop it up
        window.setLocalTranslation(400, 400, 100);
        //    getState(PopupState.class).showPopup(window, closeCommand);
        // Schließt, wenn außerhalb geklickt wurde und führt zusätzlich das übergebene Command (closeCommand) aus
        //        getState(PopupState.class).showPopup(window); //würde das Command nicht ausführen
        // Also so ähnlich wie die on Close Methode...

        // Man muss im PopUpState nach der richtigen Methode suchen! Nachfolgend z.B. bleibt der Fokus auf dem GUI Element
        //     getState(PopupState.class).showPopup(window,PopupState.ClickMode.Consume, closeCommand,ColorRGBA.randomColor());
        //     PopupState.ClickMode.ConsumeAndClose; --> Geht weg und der andere Klick wird weiter benutzt
        //    Lemur\src\main\java\com\simsilica\lemur\event\PopupState.java
        // Es wird also im Hintergrund ein Objekt gesetzt

        getState(PopupState.class).showPopup(window,PopupState.ClickMode.Consume, closeCommand,null);

        // wir können window nehmen und nicht window child txtlabel da die anderen elemente alle die clicks abfangen!
        MouseEventControl.addListenersToSpatial(window,
                new DefaultMouseListener() {
                    private boolean gedrueckt = false;
                    private int[] pos = {0,0};

                    @Override
                    public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture) {
                        if (event.isPressed() && event.getButtonIndex() == 0) {
                            gedrueckt = true;
                        } else {gedrueckt = false;}
                        pos[0] = event.getX();
                        pos[1] = event.getY();
                    }

                    @Override
                    public void mouseMoved(MouseMotionEvent event, Spatial target, Spatial capture) {
                        if (gedrueckt) {
                            window.setLocalTranslation(window.getLocalTranslation().add(event.getX()-pos[0],event.getY()-pos[1],0));
                            pos[0] = event.getX();
                            pos[1] = event.getY();
                        }
                    }

                    @Override
                    public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture) {


                    }

                    @Override
                    public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture) {
                        gedrueckt = false;
                    }

                });

        txtfieldselector pickandscrollme = new txtfieldselector();
        MouseEventControl.addListenersToSpatial(textField,pickandscrollme);

        document.reverseSelect(0,2);
        //  textField.setselectcolor(new ColorRGBA(ColorRGBA.randomColor()));
        textField.setselectcolor(new ColorRGBA(ColorRGBA.Red));

        //  textField.scale(0.5f,0.8f,1);
    }

    @Override
    protected void onDisable() {
        window.removeFromParent();
    }

    protected void home() {
        document.home(false);
    }

    protected void end() {
        document.end(false);
    }

    protected void forward() {
        document.right();
        document.getCaratLine();
    }

    protected void back() {
        document.left();
    }

    protected void insert() throws IOException, UnsupportedFlavorException {
        document.insert("add");
    }

    protected void delete() {
        document.left();
        document.delete();
    }

    /**
     *  Added this to test the bug where elements removed from the
     *  scene graph would still retain focus... thus their focus actions
     *  like 'space' to activate buttons would still be active.
     */
    protected void close() {
        System.out.println("close");
        getState(MainMenuState.class).closeChild(this);
    }

    private class CloseCommand implements Command<Object> {

        public void execute( Object src ) {
            getState(MainMenuState.class).closeChild(TextEntryDemoState.this);
        }
    }

    protected void ScrollModes() {
        switch (document.getScrollMode()) {
            case None:
                textField.setScrollMode(TextField.ScrollMode.Full);
                break;
            case Full:
                textField.setScrollMode(TextField.ScrollMode.AutoadjustX);
                break;
            case AutoadjustX:
                textField.setScrollMode(TextField.ScrollMode.AutoadjustY);
                break;
            case AutoadjustY:
                textField.setScrollMode(TextField.ScrollMode.None);
                break;
        }
        document.setText(exampleTexts());
        // we adjust text as its lenght maybe > textbox width
        textField.adjustText(0,true,false);

    }

    protected void SelectMode() {
        switch (textField.getTextselectmode()) {
            case Off:
                textField.setTextselectmode(true); //equals Auto
                break;
            case Auto:
                textField.setTextselectmode(TextField.TextselectMode.Manuell);
                break;
            case Manuell:
                textField.setTextselectmode(TextField.TextselectMode.Off);
        }
        textField.setText(exampleTexts());
        textField.adjustText(0,true,false);
    }

    protected String exampleTexts() {
        String helper="";

        switch (document.getScrollMode()) {
            case None:
                helper += "Scrollmode  \"None\" activated! - No scrolling allowed. \nLines are limited by setPreferredLineCount or setmaxLinecount";
                break;
            case Full:
                helper += "Scrollmode  \"Full\" activated! - Scrolling in X-and Y direction with offset enabled.";
                break;
            case AutoadjustX:
                helper += "Scrollmode  \"AutoadjustX\" activated!\nLines will be wrapped once they have reached borders.";
                break;
            default:
                helper += "Scrollmode \"AutoadjustY\" activated!\ntext will be wrapped once it has reached max. number of lines.";
        }
        switch (textField.getTextselectmode()) {
            case Off:
                helper +="\nTextselect mode OFF (0). No options are available.\nSelected areas wont be shown, even if added via code";
                break;
            case Auto:
                helper += "\nTextselect mode Auto (1) activated.\nSTR + A; STR + C; STR + V; STR+X available. Add, delete or inverse selected areas via code or arrow keys.";
                break;
            case Manuell:
                helper += "\nTextselect mode Manuell (2) activated.\nManipulate selected text via code only. There is no inbuild option available";
        }
        return helper;
    }

    protected void adjust_and_addAreas() {
        if (textField.isTextselect()) {
            int i;
                /*
                we just adjust the current text
                note that this the wrapping is done by character not words
                the adjust in scrollMode Full and AutoadjustX is using wordwrap ()

                we get the lenght of the whole text (not only the visible text!)
                and add multiple anchors over the lenght of the text
                try the add, del and reverse functionality in textselect mode Auto!
                */

            textField.adjustText(1,true,false);
            int [] xx = {0,0};
            System.out.println(document.getoffsetText(xx ,false).length());
            int jj = document.getoffsetText(xx ,false).length();
            for (i =0; i<jj;i=i+10 ) {
                document.addTextselectArea(i,i+4);
            }
        }
    }

    protected void Color() {
        // our textselect can have different colors
        // please note, that the alpha will be ignored and is always 0.25
        if (textField.getselectColor().equals(new ColorRGBA(0,0,255,0.25f))) {
            textField.setselectcolor(new ColorRGBA(ColorRGBA.randomColor()));
        } else {
            textField.setselectcolor(new ColorRGBA(0,0,255,0.25f));
        }
    }


    protected void OffsetY() {
        document.setOffset_Y(2);
    }

    protected void readonly() {
         textField.setreadonly(!textField.getReadonlymode());
    }

    private class txtfieldselector extends DefaultMouseListener {
        private boolean press = false;
        private boolean press2 = false;
        private int before =0;
        private int nextstep = 0;
        private long zeit = System.currentTimeMillis();


        @Override
        public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture) {
            if (!(target instanceof TextField)) return;
            TextField textField = (TextField) target;
            DocumentModel document = textField.getDocumentModel();
            if (event.isPressed()) {

                // on click + all selected = we empty the selections
                if (document.getAnchors() != null) {
                    if (document.getAnchors().size() ==1){
                        int[] II = document.getAnchors().get(0);
                        if ((II[0] == 0) && (II[1] == document.getfulltext().length()))  {
                            document.emptyAnchors();
                            zeit-=500;
                        }
                    }
                }

                int [] p = {event.getX(),event.getY()};
                p =  textField.getTextlinesbyCoordinates(p);
                // if we have a new position
                if (!(null ==  document.findCaratValue(p))) {
                    int cvalue = (int) document.findCaratValue(p);
                    document.updateCarat(true, cvalue, true);
                }
                //  initialstart =document.getCarat();
                before = document.getCarat();;
                if (event.getButtonIndex() == 0)  press = true;
                if (event.getButtonIndex() == 1) press2 = true;

                // set Focus if we dont have it
                if (textField !=  GuiGlobals.getInstance().getFocusManagerState().getFocus()) {
                    GuiGlobals.getInstance().getFocusManagerState().setFocus(target);
                }


            } else {
                press = false;
                press2 = false;
                if (System.currentTimeMillis()-zeit < 500){ //Double click
                    document.addTextselectArea(0,document.getfulltext().length());
                }
                zeit = System.currentTimeMillis();
            }
        }

        @Override
        public void mouseMoved(MouseMotionEvent event, Spatial target, Spatial capture) {
            if (!(target instanceof TextField)) return;
            TextField textField = (TextField) target;
            DocumentModel document = textField.getDocumentModel();
            // move the textfield by mousewheel and get the new position if right click is used
            if (!(event.getDeltaWheel() ==0)) {
                if (event.getDeltaWheel() >0) {
                    document.up();
                } else {
                    document.down();
                }
                if (press2) nextstep = document.getCarat();



            } // get the new position if mouse is moved and left clicked
            if ((press) && (event.getDeltaWheel() ==0)) {
                int [] p = {event.getX(),event.getY()};
                p =  textField.getTextlinesbyCoordinates(p);
                // if we have a new position
                if (!(null ==  document.findCaratValue(p))) {
                    nextstep = (int) document.findCaratValue(p);
                }
            }

            if ((press || (press2 && !(event.getDeltaWheel() ==0))) && !(before == nextstep)) {
                document.reverseSelect(before,nextstep);
                before = nextstep;
            }
        }

        @Override
        public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture) {

        }

        @Override
        public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture) {
        }
    }

}



