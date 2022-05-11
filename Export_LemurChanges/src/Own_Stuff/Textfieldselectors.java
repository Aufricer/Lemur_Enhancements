package Own_Stuff;

import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.GuiGlobals;
import Lemur_Main_Files.TextField;
import com.simsilica.lemur.event.DefaultMouseListener;
import Lemur_Main_Files.DocumentModel;

        /*
        A mouselistener that once attached to a Textfield will allow the user to select and deselect text
        it will also set the position of the cursor to the last position clicked
        options will apply if textselect mode is active and/or a preferred linecount is set to textfield
         */

public class Textfieldselectors extends DefaultMouseListener {
    private boolean press = false;
    private boolean press2 = false;
    private int before =0;
    private int nextstep = 0;
    private long zeit = System.currentTimeMillis();
    private boolean looseFocus = true;

    public Textfieldselectors() {
    }

    public Textfieldselectors(boolean dontlooseFocus) {
        this.looseFocus = !dontlooseFocus; // make loosing the focus on Mouseexit optional
    }


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
        press = false;
        press2 = false;
        if (looseFocus)   GuiGlobals.getInstance().getFocusManagerState().setFocus(null);
    }
}
