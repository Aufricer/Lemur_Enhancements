package Own_Stuff;

import Editor.Config_AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import Lemur_Main_Files.TextField;
import com.simsilica.lemur.event.DefaultMouseListener;
import org.lwjgl.opengl.Display;

public class ReadFieldDraggers extends DefaultMouseListener{
    // A copy of Draghandler() more or less
    // Detects clicks and movements on a readField
    // Chooses if the element has to be moved or resized, changing cursor accordingly
    // If resizing is allowed at all, a double click will scale back the element to its normal
    // dimensions in two steps (1st make x and y scale equal, 2nd scale back to 100 %)
    // works with right mouse click

    private Boolean Klick =false;
    private int Taste;
    private Vector2f start;
    private Vector2f start2;
    private long time;
    private long time2;
    private AppStateManager localstateManager;
    private boolean resize;

    public ReadFieldDraggers(AppStateManager manager) {
        this(manager,false);
        }

    public ReadFieldDraggers (AppStateManager manager, boolean allow_resizing){
        localstateManager = manager;
        this.resize = allow_resizing;
    }

    int counter = 0;
    @Override
    protected void click(MouseButtonEvent event, Spatial target, Spatial capture) {
       if  (Taste == 1 && System.currentTimeMillis() - time < 500) { // Kurzer Klick
           counter++;
       }
       if (counter == 1)   time2 = System.currentTimeMillis();
       if (counter == 2) {
           // Doppelklick ?
           if (((time - time2) < 600) && resize){
               Spatial move = target;
               if (target.getClass().equals(TextField.class)) move = move.getParent().getParent();
               float x = ((Elemente.ReadField) move).getLocalScale().x;
               float y = ((Elemente.ReadField) move).getLocalScale().y;
               if (x == y) { //Skalierung des Elements in 2 Schüben normalisieren
                   ((Elemente.ReadField) move).setLocalScale(1f,1f,1f);
               } else {
                   x = Math.max(x,y);
                   ((Elemente.ReadField) move).setLocalScale(x,x,1f);
               }
           }
           counter = 0;
       }

    }

      @Override
    protected boolean isClick(MouseButtonEvent event, int xDown, int yDown) {
          return super.isClick(event, xDown, yDown);
    }


    int i =0;
    int Klicktyp = 0;
    @Override
    public void mouseButtonEvent(MouseButtonEvent event, Spatial target, Spatial capture) {
        Klick = event.isPressed();
        Taste = event.getButtonIndex();
        if (Klick) {
            this.start = new Vector2f(event.getX(),event.getY());
            this.time =System.currentTimeMillis();
            i=1;
            Klicktyp = 0;
            start2 = start.clone();
        } else {
            localstateManager.getState(Config_AppState.class).MouseCursorChooser(Config_AppState.MouseCursorTypes.Standard);
            if (isClick(event,(int) start2.getX(), (int) start2.getY())) {
                click(event,target,capture);
            }
        }
    }

    @Override
    public void mouseEntered(MouseMotionEvent event, Spatial target, Spatial capture) {
        super.mouseEntered(event, target, capture);
    }

    @Override
    public void mouseExited(MouseMotionEvent event, Spatial target, Spatial capture) {
        super.mouseExited(event, target, capture);
    }

    @Override
    public void mouseMoved(MouseMotionEvent event, Spatial target, Spatial capture) {
        // System.out.println("Mousebewegung " + event.getX());
        event.setConsumed();
        super.mouseMoved(event, target, capture);
        // Rechtsklick und etwas Verzögerung
        if (Klick && Taste == 1 && System.currentTimeMillis() - time > 750) {
            Vector2f current = new Vector2f(event.getX(), event.getY());
            Vector2f delta = current.subtract(start);
            Spatial move = target;

            if (target.getClass().equals(TextField.class)) {
                // headline oder textField -> parent ist der Container, parent davon das ReadField
                move = move.getParent().getParent();
            }

            Elemente.ReadField rFtemp = (Elemente.ReadField) move;

            Vector2f Maße = new Vector2f();
            BoundingBox b = (BoundingBox) rFtemp.getWorldBound();

            Maße.x = b.getXExtent(); // aktuelle Breite inkl. Skalierung
            Maße.y = b.getYExtent(); // aktuelle Höhe, inkl. Skalierung

            double minx = 1-Math.max(10/(Maße.x * 2 ), 0.04);     //Mindestens 10 oder 4 %
            double miny = Math.max(10/ (Maße.y*2),0.04);
            if ((event.getX() > (rFtemp.getLocalTranslation().x + Maße.x * 2 * minx) && (event.getY() > rFtemp.getLocalTranslation().y - Maße.y * 2 *miny)) && resize) {
               //Skalieren, aber nur wenn Resize erlaubt ist
                if (i == 1) {
                    localstateManager.getState(Config_AppState.class).MouseCursorChooser(Config_AppState.MouseCursorTypes.Diagonal_right_oriented);
                    i = 0;
                    Klicktyp = 1;
                }
                if (!(Klicktyp == 1)) return;
                // Beim Skalieren ändert sich die Y Position sowie die Größe des Feldes in X und Y Richtung
                float x_in_rF = start.getY()-(rFtemp.getLocalTranslation().y - Maße.y*2);
                float x2_in_rF = event.getY()-(rFtemp.getLocalTranslation().y - Maße.y*2);
                float l2 = (Maße.y*2)/(x_in_rF/x2_in_rF);  // stauchung = (x_in_rF/x2_in_rF);, Differenz der Länge zur akteullen y pos (l2 - (Maße.y*2)
             //   float normal = (Maße.y*2)/rFtemp.getLocalScale().y;
                float neuscale_y = l2/((Maße.y*2)/rFtemp.getLocalScale().y);
             //    neue Breite  = event.getX() + delta.getX() - rFtemp.getLocalTranslation().x)
             //    alte Breite = (event.getX() - rFtemp.getLocalTranslation().x)

                if (rFtemp.getLocalScale().x * (event.getX()+delta.getX()-rFtemp.getLocalTranslation().x)/(event.getX() -rFtemp.getLocalTranslation().x) > 0.25 && neuscale_y >= 0.25) {
                    rFtemp.setLocalScale(rFtemp.getLocalScale().x * (event.getX() + delta.getX() - rFtemp.getLocalTranslation().x) / (event.getX() - rFtemp.getLocalTranslation().x), neuscale_y, 1f);
                    rFtemp.setLocalTranslation(rFtemp.getLocalTranslation().addLocal(0f,l2-(Maße.y)*2,0f));
                }
           start = current;

            }else { // Bewegen, wenn nicht resized wird
               if (i == 1) {
                  localstateManager.getState(Config_AppState.class).MouseCursorChooser(Config_AppState.MouseCursorTypes.Move);
                  i = 0;
                   Klicktyp = 2;
               }
               if (!(Klicktyp == 2)) return;
               Vector3f zielposition = null;
               zielposition = move.getLocalTranslation().addLocal(new Vector3f(delta.x, delta.y, 0f));
               zielposition.setX(Math.min(Display.getWidth() - Maße.x, zielposition.x)); // halbes Feld außerhalb erlaubt
               zielposition.setX(Math.max(-Maße.x, zielposition.x));
               zielposition.setY(Math.min(Display.getHeight(), zielposition.y)); // Rahmen oben
               zielposition.setY(Math.max(Maße.y, zielposition.y)); //halbes Feld nach unten

               move.setLocalTranslation(zielposition);
               start = current;
            }


            }
        }


    }
