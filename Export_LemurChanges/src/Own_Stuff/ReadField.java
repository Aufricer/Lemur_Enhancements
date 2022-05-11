package Own_Stuff;

import Lemur_Main_Files.TextField;
import ViewPortPanel.ViewportPanel;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.light.AmbientLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;
import com.simsilica.lemur.*;
import com.simsilica.lemur.anim.SpatialTweens;
import com.simsilica.lemur.anim.TweenAnimation;
import com.simsilica.lemur.anim.Tweens;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.SpringGridLayout;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.*;
import com.simsilica.lemur.style.*;
import Lemur_Main_Files.DocumentModel;

import java.util.concurrent.Callable;

import static Lemur_Main_Files.TextEntryComponent.STRC;

public class ReadField extends Panel {

    public TextField textField;
    public TextField headline;
    private Container tfContainer;
    private Container headlineContainer;
    public ViewportPanel VPscenebox;
    public Container picture;

    private float elementwidth;
    private float elementheight;
    private float elementpicturewidth;
    private float preferredelementpicturewidth;
    private float elementpictureheight;
    private float preferredelementpictureheight;
    private boolean preferredshowheadline =false;
    private boolean showheadline = false;
    private boolean preferredshowimage = false;
    private boolean showimage = false;
    private boolean updateneed =false;
    private boolean sliderbehaviour_normal = true;
    private boolean scaleimage = false;


    public static final String ELEMENT_ID = "readField";
    private static final String TextField_ID = "textField";
    private static final String CONTAINER_ID = "container";
    private static final String SLIDER_ID = "slider";
    private static final String Headline_ID = "headline";


    private BorderLayout layout;
    private VersionedReference<DocumentModel> tfmodelref;
    private VersionedReference<Integer> tfcaratref;
    private int lastcaratline = 0;


    private Slider slidervert;
    private RangedValueModel baseIndex;
    private VersionedReference<Double> sliderref;


    private Textfieldselectors selector;


    // only text
    public ReadField(String style, String text) {
        this(true,new ElementId(ELEMENT_ID),style,text,null,false,null,null,null);
    }

    public ReadField(String text) {
         this(true,new ElementId(ELEMENT_ID),null,text,null,false,null,null,null);
    }

    // text and headline
    public ReadField(String style, String text, String headline, boolean showheadline) {
        this(true,new ElementId(ELEMENT_ID),style,text,headline,showheadline,null,null,null);
    }

    public ReadField(String text, String headline, boolean showheadline) {
        this(true,new ElementId(ELEMENT_ID),null,text,headline,showheadline,null,null,null);
    }

    // text, optional headline and optional scene
    public ReadField(String style, String text, String headline, boolean showheadline, Spatial scene, AppStateManager manager) {
        this(true,new ElementId(ELEMENT_ID),style,text,headline,showheadline,manager,scene,null);
     }

    public ReadField( String text, String headline, boolean showheadline, Spatial scene, AppStateManager manager) {
        this(true,new ElementId(ELEMENT_ID),null,text,headline,showheadline,manager,scene,null);
    }

    // text, optional headline and optional picture
    public ReadField(String style, String text, String headline, boolean showheadline, Texture image) {
          this(true,new ElementId(ELEMENT_ID),style,text,headline,showheadline,null,null,image);
    }

    public ReadField(String text, String headline, boolean showheadline, Texture image) {
        this(true,new ElementId(ELEMENT_ID),null,text,headline,showheadline,null,null,image);
    }

    protected ReadField(boolean applyStyles, ElementId elementId, String style,String text, String headlinetxt, boolean showhead, AppStateManager stateManager, Spatial scene,  Texture txtu) {
        // we dont apply styles for the super type but then we do it at the end
        super(false, elementId, style);

        // make basic layout
        this.layout = new BorderLayout();
        getControl(GuiControl.class).setLayout(layout);
        this.preferredshowheadline = showhead;
        showheadline = showhead;

    // We create 2 containers for headline + image or scene and text
        headlineContainer = new Container(new SpringGridLayout(Axis.Y,Axis.X,FillMode.None,FillMode.None),new ElementId(Headline_ID).child(elementId).child(CONTAINER_ID), style);
        tfContainer = new Container(elementId.child(CONTAINER_ID), style);
        layout.addChild(headlineContainer,BorderLayout.Position.North);
        layout.addChild(tfContainer, BorderLayout.Position.Center);

    // add textfield and selectors
        textField = new TextField(text, elementId.child(TextField_ID), style);
        textField.adjustText(0,true,true);
        tfmodelref = textField.getDocumentModel().createReference();
        tfcaratref = textField.getDocumentModel().createCaratReference();
        tfContainer.addChild(textField);
        selector = new Textfieldselectors();
        MouseEventControl.addListenersToSpatial(textField, selector);
        textField.setreadonly(true); // This can be set via style but to be sure we set it here again


        // prepare slider and reference
        baseIndex = new DefaultRangedValueModel();
        sliderref = baseIndex.createReference();
        baseIndex.setMaximum(10);
        baseIndex.setValue(1);
        slidervert = new Slider(baseIndex, Axis.Y, elementId.child(SLIDER_ID), this.getStyle());

        // prepare a headline note it must be set active elsewhere
        ElementId id = new ElementId(Headline_ID).child(elementId.child(TextField_ID));
        headline = new TextField("", id, style);
        headline.getActionMap().clear();
        headline.setPreferredCursorWidth(0f);
        getHeadline().setText(headlinetxt);

        // add initial elementsize
        // can be changed by setting them or auto adjusted while updating and recalculating Gui element sizes
        // note that in a first step only the textfield is used to determine sizes

        // Height is set as 10 % headline, 40% for picture or scene and 50 % for the text
        // width - the scene will take 80 % of the available width
        // as the width is

        elementheight = tfContainer.getPreferredSize().getY()*2;
        elementwidth = tfContainer.getPreferredSize().getX();

        // initial scene sizes
        preferredelementpictureheight = elementheight*0.4f;
        preferredelementpicturewidth = elementwidth*0.8f;


          // We prepare a ViewPortPanel to show
        if (!(stateManager == null)) {
          ElementId subid = new ElementId(ViewportPanel.ELEMENT_ID).child(ELEMENT_ID).child(Container.ELEMENT_ID);
          VPscenebox = new ViewportPanel(stateManager, subid, style);
          setViewPortPanelScene(scene);
          preferredshowimage = true;
          elementpictureheight=preferredelementpictureheight;
          elementpicturewidth = preferredelementpicturewidth;
        }
        // We prepare a picture to show
        if (!(txtu == null)) {
            picture = new Container(style);
            QuadBackgroundComponent QBG = new QuadBackgroundComponent(txtu);
            picture.setBackground(QBG);
            preferredshowimage = true;
            // for pictures we adjust the actual picturesize to the preferred sizes
            float q = Math.min(preferredelementpictureheight/txtu.getImage().getHeight(),preferredelementpicturewidth/txtu.getImage().getWidth());
            elementpictureheight = q*txtu.getImage().getHeight();
            elementpicturewidth = q* txtu.getImage().getWidth();

        }

        // apply our individual Readfield styles
        // those can be applied from inside this class or from a style
        if (applyStyles) {
            Styles styles = GuiGlobals.getInstance().getStyles();
            styles.applyStyles(this, getElementId(),style);
        }
        showHeadline();
        showPictureorScene();
        updateallsizes();
      //  textField.getDocumentModel().updateCarat(false, 0, true); // carat +0, um caratupdate -> resetcursorposition -> autoadjust textfield
        textField.getDocumentModel().end(false);
        textField.getDocumentModel().home(false);
    }

    private void checkslider() {
        // available lines vs. lines to show
        if (textField.getDocumentModel().getLineCount() <= textField.getPreferredLineCount()) {
            // no slider needed
            if (!(slidervert.getParent() == null)) {
                layout.removeChild(slidervert);
                updateneed = true;
            }
        } else {
            // slider is (still) needed
            // calculate and set new slider values
            updateslidermax();
            updateslidervalue();
            if (slidervert.getParent() == null) { // add slider, if not yet attached
                layout.addChild(slidervert, BorderLayout.Position.East);
                updateneed = true; // triggers   textField.adjustText(0,true,true); + sizeadjust via updateloop if needed
            }

        }
    }

    public void enable_Copyfrom_textField(boolean allow) {
        if (allow) {
            textField.getActionMap().put(new KeyAction(KeyInput.KEY_C, KeyModifiers.CONTROL_DOWN), STRC);
        } else {
            textField.getActionMap().remove(new KeyAction(KeyInput.KEY_C, KeyModifiers.CONTROL_DOWN), STRC);
        }
    }
    public void enable_Dragging (boolean Dragable, AppStateManager lstateManager) {
        enable_Dragging(Dragable,lstateManager,false);
    }

    public void enable_Dragging (boolean Dragable, AppStateManager lstateManager, boolean resizing) {
        // unfortunately Dragging with Draghandler from com.simsilica.lemur.event only works for the container part of
        // our readField. Thus we had to find a solution for the (optional) hedline and non optinal textField
        // Thus we attach the custom made ReadFieldDragger.
        // Note that the ReadFieldDragger needs the AppstateManager to choose and select the (mouse) cursor

        ReadFieldDraggers dr = new ReadFieldDraggers(lstateManager,resizing);
        if (Dragable) {
         //   CursorEventControl.addListenersToSpatial(this, new DragHandler());
            MouseEventControl.addListenersToSpatial(this,dr);
            MouseEventControl.addListenersToSpatial(this.getTextfield(),dr);
            MouseEventControl.addListenersToSpatial(this.getHeadline(),dr);
        } else {
            MouseEventControl.removeListenersFromSpatial(this,dr);
            MouseEventControl.removeListenersFromSpatial(textField,dr);
            MouseEventControl.removeListenersFromSpatial(headline,dr);

        }
    }

    public float getElementwidth() {
        return elementwidth;
    }

    public float getElementheight() {
        return elementheight;
    }

    public TextField getHeadline() {
        return headline;
    }

    private float getRatio() {
        float ratio = 1f;
        if (!(picture == null)) {
            QuadBackgroundComponent QB = (QuadBackgroundComponent) picture.getBackground();
            Texture txt = QB.getTexture();
            float breite = txt.getImage().getWidth();
            float hoehe = txt.getImage().getHeight();
            ratio = breite / hoehe;
        }
        return ratio;
    }

    public ViewportPanel getViewportPanel() {
        return VPscenebox;
    }

    public TextField getTextfield() {
        return textField;
    }

  // styles.applyStyles(this, elementId.getId(), style);
    @StyleDefaults(ELEMENT_ID)
    public static void initializeDefaultStyles(Styles styles, Attributes attrs) {
/*      This function is called upon calling the styles.applyStyles function
        We make some standard settings here. If you have an AppState (or anything) setting the styles
        those entries here won't be overridden
        Please Note: as of Lemur 1.15 there is a bug. At least I believe its a bug paulspeed said it might be intented
        That "bug" is preventing you to get the right attributes at this place. You may use e.g.
          styles.getSelector(this.getElementId(), styles.getDefaultStyle())"/your style" .set("setReadfieldPreferredWidth", 50, false);
        insteadt of
         attrs.set  attrs.set("setReadfieldPreferredWidth",50,false);
         settings of substyles (e.g. readfield.slider) need to be done with the styles.getSelector notation

         First come settings from style, then those below are called and overwritten or not
         followed by calling all available Styleattributes (matched below)
 */
        attrs.set("setReadfieldPreferredWidth",50,false);
        attrs.set("setHeadlineHAlignment", HAlignment.Center, false);
        attrs.set("setReadfieldimagewidth", 0f, false);
        attrs.set("setReadfieldimageheight", 0f, false);
        attrs.set("setReadField_keepOriginalImagesize_ratios", false, false);
 //       attrs.set("showHeadline", true, true);
 }

    private void makeContainer(){
        // we delete all containers
        Container X = (Container) ((SpringGridLayout) headlineContainer.getLayout()).getChild(1, 0);
        if (!(X == null))   headlineContainer.removeChild(X);

        Container sub = new Container(new SpringGridLayout(Axis.X,Axis.Y,FillMode.Last,FillMode.None));
        sub.setBackground(null); // just in case style sets an bg here
        Container sub1 = new Container();
        sub1.setBackground(null);
        Container sub2 = new Container();
        sub2.setBackground(null);
        Container sub3 = new Container();
        sub3.setBackground(null);

        sub.addChild(sub1);
        sub.addChild(sub2);
        sub.addChild(sub3);

        headlineContainer.addChild(sub ,1,0);
    }

    @StyleAttribute(value = "setHeadlineHAlignment", lookupDefault = false)
    public void setHeadlineHAlignment(HAlignment hAlignment) {
        headline.setTextHAlignment(hAlignment);
    }

    @StyleAttribute(value = "setReadfieldPreferredHeight", lookupDefault = false)
    public void setReadFieldPreferedHeight(float height) {
        height = Math.max(0,height);
        if (!(elementheight == 0.0f))    preferredelementpictureheight = preferredelementpictureheight * (height/elementheight);
        elementheight = height;
        updateneed = true;
    }

    // sets a new width for the image or scene in an Readfield
    @StyleAttribute(value = "setReadfieldimagewidth", lookupDefault = false)
    public void setReadfieldimagewidth(float width) {
        width = Math.min(width,elementwidth);
        preferredelementpicturewidth = Math.max(0,width);
        if (!(VPscenebox== null))   VPscenebox.setPreferredSize(new Vector3f(preferredelementpicturewidth,elementpictureheight,1));
        if (!(picture== null)) {
            if (scaleimage) {
                preferredelementpictureheight = Math.min(preferredelementpicturewidth / getRatio(), elementheight);
            }
            picture.setPreferredSize(new Vector3f(preferredelementpicturewidth, preferredelementpictureheight, 1));
        }
        updateneed = true;
    }

    // changed position so this Styleattribute is called before imagewidth is set, thus the width stays correct
    @StyleAttribute(value = "setReadfieldPreferredWidth", lookupDefault = false)
    public void setReadfieldPreferredWidth(float width) {
        width = Math.max(0, width);
        if (!(elementwidth == 0.0f)) preferredelementpicturewidth = preferredelementpicturewidth*(width/elementwidth);
        elementwidth = width;
        updateneed = true;
    }

    public void setReadfieldimagewidth(double valueaspercent) {
        valueaspercent = valueaspercent*elementwidth/100;
        setReadfieldimagewidth(valueaspercent);
    }

    @StyleAttribute(value = "setReadfieldimageheight", lookupDefault = false)
    public void setReadfieldimageheight(float height) {
        height = Math.min(height,elementheight);
        preferredelementpictureheight = Math.max(0,height);
        // set the elment
        if (!(VPscenebox== null))   VPscenebox.setPreferredSize(new Vector3f(elementpicturewidth,preferredelementpictureheight,1));
        if (!(picture== null))   {
            if (scaleimage) {
                preferredelementpicturewidth = Math.min(preferredelementpictureheight*getRatio(),elementwidth);
            }
            picture.setPreferredSize(new Vector3f(preferredelementpicturewidth,preferredelementpictureheight,1)); //maybe not necessary at all, as we calculate the sizes right afterwards
        }
        // check sizes
        updateneed = true;
    }

    public void setReadfieldimageheight(double valueaspercent) {
        valueaspercent = valueaspercent*elementheight/100;
        setReadfieldimageheight(valueaspercent);
    }

    // Once enabled changing the height or width will always change the
    // preferred sizes of both the width and height (picture only) while keeping the ratio
    @StyleAttribute(value = "setReadField_keepOriginalImagesize_ratios", lookupDefault = false)
    public void setReadField_keepOriginalImagesize_ratios(boolean keep){
        scaleimage = keep;
    }


    private void setViewPortPanelScene(Spatial scene) {
        if (VPscenebox == null) return;
        Callable<Object> loadScene = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                VPscenebox.detachAllScenes();
                if (scene == null) return true;
                AmbientLight L1 = new AmbientLight();
                L1.setColor(ColorRGBA.White.mult(4f));
                VPscenebox.addLight(L1);
                VPscenebox.attachScene(scene);
                // ToDo Tween auswählbar machen
                GuiGlobals.getInstance().getAnimationState().
                        add(new TweenAnimation(true, Tweens.smoothStep(SpatialTweens.rotate(VPscenebox.getViewportNode(), new Quaternion().fromAngleAxis(-FastMath.QUARTER_PI, Vector3f.UNIT_Y), new Quaternion().fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_Y), 2)),
                                Tweens.smoothStep(SpatialTweens.rotate(VPscenebox.getViewportNode(), new Quaternion().fromAngleAxis(FastMath.QUARTER_PI, Vector3f.UNIT_Y), new Quaternion().fromAngleAxis(-FastMath.QUARTER_PI, Vector3f.UNIT_Y), 2))));
                return true;
            }
        };
        try {
            loadScene.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setViewPortPanel(ViewportPanel vp) {
        if (VPscenebox == null) picture = null; // to be sure we set image null
        VPscenebox = null;
        VPscenebox = vp;
        showPictureorScene();
        // ToDo Test me
    }

    @StyleAttribute(value = "showHeadline", lookupDefault = false)
    public void showHeadline(boolean show) {
        preferredshowheadline =show;
        showheadline = show;
        showHeadline();
    }

    private void showHeadline() {

        if ((showheadline) && (headline.getParent() == null)) {
            headlineContainer.addChild(headline, 0,0);
            updateneed = true;
            return;
        }
        if (!(showheadline) && (!(headline.getParent() == null))) {
            headlineContainer.removeChild(headline);
            updateneed = true;
        }

    }

    public void showPictureorScene(boolean show){
        preferredshowimage = show;
        showimage = show;
        showPictureorScene();
    }

    private void showPictureorScene(){
        if ((picture == null) && (VPscenebox == null)) return; // no image there
        if (!(VPscenebox==null)) { // scene
            if ((showimage) && (VPscenebox.getParent() == null)) { // not attached scene
                makeContainer();
                Container tc = (Container) (( SpringGridLayout ) headlineContainer.getLayout()).getChild(1, 0);
                ((Container) ((SpringGridLayout) tc.getLayout()).getChild(1, 0)).addChild(VPscenebox);
                VPscenebox.setPreferredSize(new Vector3f(elementpicturewidth,elementpictureheight,1));
                updateneed = true;
            }
            if ((!(showimage)) && (!(VPscenebox.getParent() == null))) { // attached scene
                Container tc = (Container) (( SpringGridLayout ) headlineContainer.getLayout()).getChild(1, 0);
                ((Container) ((SpringGridLayout) tc.getLayout()).getChild(1, 0)).removeChild(VPscenebox);
                Container X = (Container) ((SpringGridLayout) headlineContainer.getLayout()).getChild(1, 0);
                if (!(X == null))   headlineContainer.removeChild(X);
                updateneed = true;
            }
        return;
        }
        if (!(picture==null)) { // picture
            if ((showimage) && (picture.getParent() == null)) { // not attached picture
                makeContainer();
                Container tc = (Container) (( SpringGridLayout ) headlineContainer.getLayout()).getChild(1, 0);
                ((Container) ((SpringGridLayout) tc.getLayout()).getChild(1, 0)).addChild(picture);
                picture.setPreferredSize(new Vector3f(elementpicturewidth,elementpictureheight,1));
                updateneed = true;
            }
            if ((!(showimage)) && (!(picture.getParent() == null))) { // attached picture
                Container tc = (Container) (( SpringGridLayout ) headlineContainer.getLayout()).getChild(1, 0);
                ((Container) ((SpringGridLayout) tc.getLayout()).getChild(1, 0)).removeChild(picture);
                Container X = (Container) ((SpringGridLayout) headlineContainer.getLayout()).getChild(1, 0);
                if (!(X == null))   headlineContainer.removeChild(X);
                updateneed = true;
            }
            return;
        }

    }

    private void updateallsizes(){
        // each time a subelement of ReadField is changing its size we recalculate the sizes of all subelements
        // the goal is to prevent exceptions, to match the given element and scene sizes,
        // to have the scene or picture not stretched unnormal and to have the ReadFields total size equal

        // we always try to set the scene/ image size to the preference,
        // they could become smaller but never bigger as given
        // If it is necessary to shrink the scene it will be done to "save" the headline
        // Size not needed by the headline or scene will stretch the textfield + increase its visible lines

        // Note that element sizes come from style, user input or can be autoadjustet to prevent exceptions!

        elementpictureheight = preferredelementpictureheight;
        elementpicturewidth = preferredelementpicturewidth;
        showheadline = preferredshowheadline;
        showimage = preferredshowimage;



        // scaled lineheight and number of visible lines
        float lineheight = new BitmapText(textField.getFont()).getLineHeight();
        lineheight = lineheight*(textField.getFontSize() / textField.getFont().getPreferredSize());
        int numberofvisiblelines = textField.getPreferredLineCount();

        // size of the headline
        float headlineheight = Math.max(0, headline.getPreferredSize().getY());
        headlineheight *= preferredshowheadline ? 1:0;

        // height of middle part -. the height of scene or image (the desired one, not current)
        float middleheight =0;
        if (preferredshowimage) middleheight = preferredelementpictureheight;

    /*    ToDO delete me
    if (preferredshowimage) {
            if (!(VPscenebox == null)) {
                middleheight = Math.max(VPscenebox.getPreferredSize().getY(),0);
                if (!(VPscenebox.getParent() == null))    sceneattached = true;
            }
            if (!(picture == null)){
                    middleheight = Math.max(picture.getPreferredSize().getY(),0);
                if (!(picture.getParent() == null))    sceneattached = true;
            }
        }
*/

        /* starting width the calculation of heights */

        // check 01 - our ReadField needs at least one line, to not throw an exception
        if ((elementheight-headlineheight-middleheight) <= lineheight) {
           middleheight = Math.max(elementheight -headlineheight-lineheight,0); // make scene smaller down to 0
           if (((elementheight-headlineheight-middleheight) <= lineheight) && (preferredshowheadline)) {
               showheadline =false; // disable headline
               headlineheight =0;
         //      showHeadline();
           }
           if (elementheight <lineheight) elementheight = lineheight; // change elementheight
       }  else {
            // check 02 - if there is more height then 1 line we check for the size of headline and scene
            // headline has priority
           float heightforscene =  elementheight  - numberofvisiblelines*lineheight - headlineheight;
            if (heightforscene > preferredelementpictureheight) {
               middleheight = preferredelementpictureheight;
           } else {
               // we need to reduce the height of the scene or if thats not enough even the headline
               float reducelines = (preferredelementpictureheight - heightforscene)/lineheight;
               reducelines = (float) Math.ceil(reducelines);
               reducelines = Math.min(reducelines,numberofvisiblelines-1);
               heightforscene = elementheight  - (numberofvisiblelines-reducelines)*lineheight - headlineheight;
               if (heightforscene > 0) {
                   middleheight = heightforscene;
               } else {
                   // we remove the headline (and save the height if there was some at all)
                   showheadline = false;
              //     showHeadline();
                   heightforscene += headlineheight;
                   headlineheight = 0;
                   middleheight = heightforscene; // we could remove the scene as well as it might be super small but we dont know how big the headline would be
               }
           }
        }
        showHeadline();

        // Check 3 - is there still space for a scene
        // if we dont like to show a scene/image or if we dont have a size we just remove it here
        if ((middleheight <= 0) & (preferredshowimage) || (!preferredshowimage)) {
            showimage = false;
            middleheight = 0;
        }

        // check 4 - We get the number of new lines for our ReadField
        float newnumberoflines = (elementheight-headlineheight-middleheight)/lineheight;
        newnumberoflines = (float) Math.floor(newnumberoflines);
        textField.setPreferredLineCount((int) newnumberoflines); // in element value will never be < 1
        textField.setmaxLinecount(textField.getPreferredLineCount());

        // set the new heights of the Textfield (could be more than a multiply of lineheight)
        float newtfheight = Math.max(lineheight,elementheight-headlineheight-middleheight);

        // and we set the height of the scene + add(remove it if needed)
        elementpictureheight =middleheight;
        showPictureorScene();

        /* now we check for the width of the elements */

        // check 05 - do we have a slider ? If yes he will be added right to the element and take a certain width
        float sliderwidth=0;
        if (!(slidervert.getParent() == null)) {
            sliderwidth =slidervert.getPreferredSize().getX();
        }

        elementwidth = Math.max(elementwidth,sliderwidth); //make element bigger if needed

        // width of the center of the layout (the slider is attached on east)
        float layoutcenterwidth;
        layoutcenterwidth = elementwidth - sliderwidth;

        if (elementpicturewidth > layoutcenterwidth) {
            elementpicturewidth = layoutcenterwidth; // change width of picture
        }
        /* Check 06 - set the sizes of the containers
           we have 3 Containers in the middle (1 left, 1 central and 1 right)
           central container will take the scene or picture
           the left container is occupying space to have the scene central in the element
           the right has fillmode.last -->  so we just need to set the size of the first container */

        float spacerwidth = 0;
        if (!(VPscenebox == null)) {
            if (!(VPscenebox.getParent() == null))  spacerwidth = (elementwidth/2) - (elementpicturewidth/2);
        }

        if (!(picture == null)){
            if (!(picture.getParent() == null)) spacerwidth = (elementwidth/2) - (elementpicturewidth/2);
        }

        // Final Step: Set all sizes

        headline.setPreferredWidth(layoutcenterwidth);
        textField.setPreferredWidth(layoutcenterwidth);
        tfContainer.setPreferredSize(new Vector3f(textField.getPreferredSize().x, newtfheight, 1f)); // Container kann abweichendes prefsize haben.

        sliderbehaviour_normal = true;
        if (!(slidervert.getParent() == null )) {
        /* unfort. scaling the slider wont work. Yes its making the slider smaller but then the textfield wont adjust properly
        I have no solution for it at the moment. So only solution is the inbuild function to auto add a bit to x and y size +
        let the user set the right size of a readField element
        slidervert.scale(slidervert.getWorldScale().x,,);
        slidervert.scale(newtfheight/slidervert.getPreferredSize().y);
        even remove and creating slider new wont work

        Another problem comes with the calculation of the slider size
        in   slider.class --> protected void resetStateView() its stated that
        y = (double)(pos.y - rangeSize.y) + visibleRange * this.model.getPercent();
        once visible range < 0 the slider will go from up to down and not the other way around
        "visibleRange" for y-axis oriented sliders is range.y (the size of the element) - the height of the thump
        depending on the result the function updateslidervalue() will calculate different values
       */
            slidervert.setPreferredSize(new Vector3f(slidervert.getPreferredSize().x, newtfheight, 1f));
            // movement behaviour of the slider
            if (slidervert.getThumbButton().getPreferredSize().y > slidervert.getRangePanel().getSize().y)  sliderbehaviour_normal = false;
        }

        if (!(VPscenebox == null))   VPscenebox.setPreferredSize(new Vector3f(elementpicturewidth, elementpictureheight, 1));
        if (!(picture == null))   picture.setPreferredSize(new Vector3f(elementpicturewidth, elementpictureheight, 1));

        Container tc = (Container) (( SpringGridLayout ) headlineContainer.getLayout()).getChild(1, 0);
        if (!(tc== null)) {
            ((Container) ((SpringGridLayout) tc.getLayout()).getChild(0, 0)).setPreferredSize(new Vector3f(spacerwidth,0,1));
        }
        updateneed = false;
    }

    // checks if the size of the slider has changed
    private boolean updateslidermax() {
        if (Math.max(1, textField.getDocumentModel().getLineCount() - 1) ==  baseIndex.getMaximum())   return false;
        baseIndex.setMaximum(Math.max(1, textField.getDocumentModel().getLineCount() - 1));
        return true;
       }

    private void updateslidervalue() {
        lastcaratline = textField.getDocumentModel().getCaratLine();
        if (sliderbehaviour_normal) {
            baseIndex.setValue(baseIndex.getMaximum() - lastcaratline);
        }
        else {
            baseIndex.setValue(lastcaratline);
        }

        sliderref.update();
    }


    @Override
    public void updateLogicalState(float tpf) {
         try {
            super.updateLogicalState(tpf);
        } catch (IllegalArgumentException e) {
          //  as the user can set sizes for the elements of the ReadField (the scene, the picture, the TextField, generell sizes)
          //  it is possible that in the update loop of controls in BorderLayout.class line 61 reshape() a negative size
          //  is calculated. Thus is thrown in Guicontroll.class setsize() line 278/310 //BorderLayout Class Line 68 and 101
          // unfortunately I could not find a way to calculate the minimum size of the elements
          // thus we take the error message that is giving us the calculated negative (or missing) size
          // this size we finally add to our minimum ReadField sizes that are used in updateallsizes()

          //  System.out.println("Error" + e);
            int pos = e.toString().indexOf("(")+1;
            int pos2 = e.toString().indexOf(",", pos);
            float xadd = (float) Float.parseFloat(e.toString().substring(pos,pos2));
            pos = pos2+1;
            pos2 = e.toString().indexOf(",", pos);
            float yadd = Float.parseFloat(e.toString().substring(pos,pos2).trim());
            if ((xadd < 0) || (yadd < 0)) {
                if (xadd< 0) elementwidth +=(xadd*-1);
                if (yadd < 0) elementheight +=(yadd*-1);
                updateneed = true; // to update textfield e.g. wordwrap and updateallsizes
            }
        }

        // check for the sizes of the ReadField
        if (updateneed) {
            textField.adjustText(0,true,true);
            updateallsizes();
        }
        // the text inside the TeaxtField had changed
        if (tfmodelref.update()) {
            checkslider(); //adds or remove slider if necessary
            if (!(textField.getDocumentModel().getCaratLine() == lastcaratline)) {
                updateslidermax();
                updateslidervalue();
            }
         }

        if (sliderref.update()) {
            /* pls note that this wont work correctly if sliderbehaviour_normal = false.
            it would require overwriting Button Commands on each check etc and a lot of extra work.
            So just ignore the strange behaviour of clicking the up and down silder button
             */
            int[] positionen = {0, 0};
            // Finde aktuelle Position
            textField.getDocumentModel().findPosition(textField.getDocumentModel().getCarat(),positionen);
            positionen[0] = (int) baseIndex.getMaximum() - ((int) baseIndex.getValue()); // neue Zeile
            positionen[1] = Math.min(positionen[1], textField.getDocumentModel().getLine(positionen[0]).length()); //neues Position in der Zeile
            Integer it = textField.getDocumentModel().findCaratValue(positionen);
            if (!(it == null)) textField.getDocumentModel().updateCarat(true, it, true); // Setze Carat an die neue Position
            textField.getDocumentModel().findPosition(textField.getDocumentModel().getCarat(), positionen);
             GuiGlobals.getInstance().getFocusManagerState().setFocus(textField);
            // ermittle das neue Y-Offset und setze es
            textField.getDocumentModel().setOffset_Y(textField.getDocumentModel().getCaratLine() - textField.getPreferredLineCount()+1);
        }

        if (tfcaratref.update()) {
            if (!(textField.getDocumentModel().getCaratLine() == lastcaratline)) {
                updateslidervalue();
            }
        }


    }
}


