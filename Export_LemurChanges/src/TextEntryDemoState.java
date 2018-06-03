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

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.ColorRGBA;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.event.PopupState;
import com.simsilica.lemur.style.ElementId;

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
        window.addChild(new Label("Word Wrapped Text", new ElementId("window.title.label")));


        // Create a multiline text field with our document model
        // its scrollmode is None and textselect is unstatic by default
        textField = window.addChild(new TextField("Initial text."));

        document = textField.getDocumentModel();
        
        // Setup some preferred sizing since this will be the primary
        // element in our GUI
        textField.setPreferredWidth(500);
        textField.setPreferredLineCount(10);
        textField.setmaxLinecount(9);
 
        // Add some actions that will manipulate the document model independently
        // of the text field
        Container buttons = window.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y)));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "home")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "end")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "forward")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "back")));
        buttons.addChild(new ActionButton(new CallMethodAction(this, "insert"))); 
        buttons.addChild(new ActionButton(new CallMethodAction(this, "delete")));


        Container button2 = window.addChild(new Container(new SpringGridLayout(Axis.X, Axis.Y)));
        button2.addChild(new ActionButton(new CallMethodAction(this, "ScrollModes")));
        button2.addChild(new ActionButton(new CallMethodAction(this, "SelectMode")));
        button2.addChild(new ActionButton(new CallMethodAction(this, "adjust_and_addAreas")));
        button2.addChild(new ActionButton(new CallMethodAction(this, "Color")));

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


        getState(PopupState.class).showPopup(window,PopupState.ClickMode.Consume, closeCommand,null);

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
            case 0:
                textField.ScrollMode(1);
                break;
            case 1:
                textField.ScrollMode(2);
                break;
            default:
                // we ignore 3 as it is the same as single line
                textField.ScrollMode(0);
        }
        document.setText(exampleTexts());
        // we adjust text as its lenght maybe > textbox width
        textField.adjustText(0,true,false);

    }

    protected void SelectMode() {
    // textSelect Modes are explained in the application example
        switch (textField.getTextselectmode()) {
            case 0:
                textField.Textselectmode(true);
                break;
            case 1:
                textField.Textselectmode(2);
                break;
            default:
                textField.Textselectmode(false);
        }
        textField.setText(exampleTexts());
        textField.adjustText(0,true,false);
    }

    protected String exampleTexts() {
        String helper="";

        switch (document.getScrollMode()) {
            case 0:
                helper += "Scrollmode 0 \"None\" activated! - No scrolling allowed. \nLines are limited by setPreferredLineCount or setmaxLinecount";
                break;
            case 1:
                helper += "Scrollmode 1 \"Full\" activated! - Scrolling in X-and Y direction with offset enabled.";
                break;
            default:
                helper += "Scrollmode 2 \"Autoadjust\" activated!\nLines will be wrapped once they have reached borders.";
        }
        switch (textField.getTextselectmode()) {
            case 0:
                helper +="\nTextselect mode deactivated (0). No options are available.\nSelected areas wont be shown, even if added via code";
                break;
            case 1:
                helper += "\nTextselect mode unstatic (1) activated.\nSTR + A; STR + C; STR + V; STR+X available. Add, delete or inverse selected areas via code or arrow keys.";
                break;
            case 2:
                helper += "\nTextselect mode static (2) activated.\nManipulate selected text via code only. There is no inbuild option available";
        }
        return helper;
    }

        protected void adjust_and_addAreas() {
            if (textField.isTextselect()) {
                int i;
                // we just adjust the current text
                // note that this time its wrapped by characters not words!
                // scrollMode Autoadjust (2) comes with wordwrap autoadjust
                textField.adjustText(0,true,false);
                // we add several anchors/ selected areas to see that it is possible
                // to have more then one selected area and to let the user
                // try with the add, del and reverse functionality of textselect (available in textselect mode 1)
                for (i =0; i<document.getText().length();i=i+10 ) {
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

    private class txtfieldselector extends DefaultMouseListener {
        private boolean press = false;
        private boolean press2 = false;
        private int before =0;
        private int nextstep = 0;


        @Override
        public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture) {
            if (event.isPressed()) {
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

            } else {
                press = false;
                press2 = false;
            }
        }

        @Override
        public void mouseMoved(MouseMotionEvent event, Spatial target, Spatial capture) {

            // move the textfield by mousewheel and get the new position if pressed
            if (!(event.getDeltaWheel() ==0)) {
                if (event.getDeltaWheel() >0) {
                    document.up();
                } else {
                    document.down();
                }
                if (press2) nextstep = document.getCarat();



            } // get the new position if mouse is moved
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





