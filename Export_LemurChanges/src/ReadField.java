import ViewPortPanel.ViewportPanel;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
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
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.style.*;

import java.util.concurrent.Callable;

public class ReadField extends Panel {

    public com.simsilica.lemur.TextField textField;
    public com.simsilica.lemur.TextField headline;
    private Container tfContainer;
    private Container headlineContainer;
    public ViewportPanel VPscenebox;
    public Container picture;

    private float elementwidth;
    private float elementheight;
    private float elementpicturewidth;
    private float elementpictureheight;
    private boolean showheadline =false;
    private boolean showimage = false;
    private boolean updateneed =false;


    public static final String ELEMENT_ID = "readField";
    private static final String TextField_ID = "textField";
    private static final String CONTAINER_ID = "container";
    private static final String SLIDER_ID = "slider";
    private static final String Headline_ID = "headline";


    private BorderLayout layout;
    private VersionedReference<com.simsilica.lemur.text.DocumentModel> tfmodelref;
    private VersionedReference<Integer> tfcaratref;
    private int lastcaratline = 0;


    private Slider slidervert;
    private RangedValueModel baseIndex;
    private VersionedReference<Double> sliderref;


    private static ElementId meid; // Need that for styling
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
    public ReadField(String text, String headline, boolean showheadline, Spatial scene, AppStateManager manager) {
        this(true,new ElementId(ELEMENT_ID),null,text,headline,showheadline,manager,scene,null);
    }

    // text, optional headline and optional picture
    public ReadField(String style, String text, String headline, boolean showheadline, Texture image) {
          this(true,new ElementId(ELEMENT_ID),style,text,headline,showheadline,null,null,image);
    }
    public ReadField(String text, String headline, boolean showheadline, Texture image) {
        this(true,new ElementId(ELEMENT_ID),null,text,headline,showheadline,null,null,image);
    }

    protected ReadField(boolean applyStyles, ElementId elementId, String style, String text, String headlinetxt, boolean showhead, AppStateManager stateManager, Spatial scene, Texture txtu) {
        // we dont apply styles for the super type but then we do it at the end
        super(false, elementId, style);

        // make basic layout
        this.layout = new BorderLayout();
        getControl(GuiControl.class).setLayout(layout);
        this.showheadline = showhead;

    // We create 2 containers for headline + image or scene and text
        headlineContainer = new Container(new SpringGridLayout(Axis.Y,Axis.X,FillMode.None,FillMode.None),new ElementId(Headline_ID).child(elementId).child(CONTAINER_ID), style);
        tfContainer = new Container(elementId.child(CONTAINER_ID), style);
        layout.addChild(headlineContainer,BorderLayout.Position.North);
        layout.addChild(tfContainer, BorderLayout.Position.Center);

    // add textfield and selectors
        textField = new com.simsilica.lemur.TextField(text, elementId.child(TextField_ID), style);
        textField.adjustText(0,true,true);
        tfmodelref = textField.getDocumentModel().createReference();
        tfcaratref = textField.getDocumentModel().createCaratReference();
        tfContainer.addChild(textField);
        selector = new Textfieldselectors();
        MouseEventControl.addListenersToSpatial(textField, selector);

        // prepare slider and reference
        baseIndex = new DefaultRangedValueModel();
        sliderref = baseIndex.createReference();
        baseIndex.setMaximum(10);
        baseIndex.setValue(4);
        slidervert = new Slider(baseIndex, Axis.Y, elementId.child(SLIDER_ID), this.getStyle());

        // prepare a headline note it must be set active elsewhere
        ElementId id = new ElementId(Headline_ID).child(elementId.child(TextField_ID));
        headline = new com.simsilica.lemur.TextField("", id, style);
        headline.getActionMap().clear();
        headline.setPreferredCursorWidth(0f);
        getHeadline().setText(headlinetxt);



        // add initial elementsize
        elementheight = tfContainer.getPreferredSize().getY();
        elementwidth = tfContainer.getPreferredSize().getX();


          // We prepare a ViewPortPanel or a picture to show
        if (!(stateManager == null)) {
          ElementId subid = new ElementId(ViewportPanel.ELEMENT_ID).child(ELEMENT_ID).child(Container.ELEMENT_ID);
          VPscenebox = new ViewportPanel(stateManager, subid, style);
          setViewPortPanelScene(scene);
          showimage = true;
          elementpicturewidth = 0.25f*elementwidth;
          elementpictureheight=0.25f*elementheight;
        }

        if (!(txtu == null)) {
            picture = new Container(style);
            QuadBackgroundComponent QBG = new QuadBackgroundComponent(txtu);
            elementpictureheight = 0.25f*elementheight;
            elementpicturewidth = Math.min(0.25f*elementwidth,0.25f*elementwidth*(txtu.getImage().getHeight()/txtu.getImage().getWidth()));
            picture.setBackground(QBG);
            showimage = true;
        }

        // apply our individual Readfield styles
        if (applyStyles) {
            Styles styles = GuiGlobals.getInstance().getStyles();
            meid = this.getElementId();
            styles.applyStyles(this, getElementId(), style);
        }

        showHeadline();
        showPicture();
        updateallsizes();
      //  textField.getDocumentModel().updateCarat(false, 0, true); // carat +0, um caratupdate -> resetcursorposition -> autoadjust textfield
        textField.getDocumentModel().end(false);
        textField.getDocumentModel().home(false);
    }



    private void checkslider() {

        if (textField.getDocumentModel().getLineCount() < textField.getPreferredLineCount()) {
            // kein slider notwendig
            if (!(slidervert.getParent() == null)) {
                layout.removeChild(slidervert);
                updateallsizes();
          //      textField.adjustText(0,true,false);
            }
        } else {
            // slider notwendig
            // ermittle und setze neu Sliderwerte
            updateslidermax();
            updateslidervalue();
            //    baseIndex.setValue(0);
            if (slidervert.getParent() == null) { // add slider, wenn notwendig
                layout.addChild(slidervert, BorderLayout.Position.East);
               // textField.getDocumentModel().updateCarat(true, 1, true);
                updateallsizes();
                updateneed = true; // triggers   textField.adjustText(0,true,true); + sizeadjust via updateloop if needed
            }
        }
    }

    public float getElementwidth() {
        return elementwidth;
    }

    public float getElementheight() {
        return elementheight;
    }

    public com.simsilica.lemur.TextField getHeadline() {
        return headline;
    }

    public ViewportPanel getViewportPanel() {
        return VPscenebox;
    }

    public com.simsilica.lemur.TextField getTextfield() {
        return textField;
    }

    @StyleDefaults(ELEMENT_ID)
    public static void initializeDefaultStyles(Styles styles, Attributes attrs) {
        // most attributes come already from the style appstate - if that one is not available
        // some standard settings can be made here
        styles.getSelector(meid, null).set("setReadfieldPreferredWidth", 50, false);
        styles.getSelector(meid, null).set("setHeadlineHAlignment", HAlignment.Center, false);
        styles.getSelector(meid, null).set("setReadfieldimagewidth", 0f, false);
        styles.getSelector(meid, null).set("setReadfieldimageheight", 0f, false);
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
/*
        sub2.setName("TEST");
        sub3.setBackground(new QuadBackgroundComponent(ColorRGBA.Pink));

        //       sub1.setPreferredSize(new Vector3f(25,50,0));
        sub1.setBackground(new QuadBackgroundComponent(ColorRGBA.Green));
        */
    }

    @StyleAttribute(value = "setHeadlineHAlignment", lookupDefault = false)
    public void setHeadlineHAlignment(HAlignment hAlignment) {
        headline.setTextHAlignment(hAlignment);
    }

    @StyleAttribute(value = "setPreferredHeight", lookupDefault = false)
    public void setPreferredHeight(float height) {
        elementheight = Math.max(0,height);
        updateallsizes();
    }

    @StyleAttribute(value = "setReadfieldPreferredWidth", lookupDefault = false)
    public void setReadfieldPreferredWidth(float width) {
       elementwidth = Math.max(0, width);
       updateallsizes();
    }

    @StyleAttribute(value = "setReadfieldimagewidth", lookupDefault = false)
    public void setReadfieldimagewidth(float width) {
        width = Math.min(width,elementwidth);
        elementpicturewidth = Math.max(0,width);
        if (!(VPscenebox== null))   VPscenebox.setPreferredSize(new Vector3f(elementpicturewidth,elementpictureheight,1));
        if (!(picture== null))   picture.setPreferredSize(new Vector3f(elementpicturewidth,elementpictureheight,1));
        updateallsizes();
    }

    public void setReadfieldimagewidth(float widthinpercent, boolean relativetoelement) {
        if (relativetoelement) widthinpercent = widthinpercent*elementwidth/100;
        setReadfieldimagewidth(widthinpercent);
    }

    @StyleAttribute(value = "setReadfieldimageheight", lookupDefault = false)
    public void setReadfieldimageheight(float height) {
        height = Math.min(height,elementheight);
        elementpictureheight = Math.max(0,height);
        // set the elment
        if (!(VPscenebox== null))   VPscenebox.setPreferredSize(new Vector3f(elementpicturewidth,elementpictureheight,1));
        if (!(picture== null))   picture.setPreferredSize(new Vector3f(elementpicturewidth,elementpictureheight,1));

        // check sizes
        updateallsizes();
    }

    public void setReadfieldimageheight(float heightinpercent, boolean relativetoReadfield) {
        if (relativetoReadfield) heightinpercent = heightinpercent*elementheight/100;
        setReadfieldimageheight(heightinpercent);
    }

    public void setRFconstantpicturesize(float maxsizeofimage) {
        if (!(picture == null)) {
            QuadBackgroundComponent QB = (QuadBackgroundComponent) picture.getBackground();
           Texture txt = QB.getTexture();
           float breite =  txt.getImage().getWidth();
           float hoehe =  txt.getImage().getHeight();

           if (hoehe > breite) {
               maxsizeofimage = Math.min(maxsizeofimage,elementheight);
               elementpictureheight = maxsizeofimage;
               elementpicturewidth = maxsizeofimage/(hoehe/breite);
           } else {
               maxsizeofimage = Math.min(maxsizeofimage,elementwidth);
               elementpicturewidth = maxsizeofimage;
               elementpictureheight = maxsizeofimage/(breite/hoehe);
           }
           picture.setPreferredSize(new Vector3f(elementpicturewidth,elementpictureheight,1f));
           updateallsizes();

        }

    }

    private void setViewPortPanelScene(Spatial scene) {
        if (VPscenebox == null) return;
        Callable<Object> loadScene = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                VPscenebox.detachAllScenes();
                if (scene == null) return true;
                AmbientLight L1 = new AmbientLight();
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
        showPicture();
        // ToDo Test me
    }

    public void showHeadline(boolean show) {
        showheadline =show;
        showHeadline();
    }

    private void showHeadline() {

        if ((showheadline) && (headline.getParent() == null)) {
            headlineContainer.addChild(headline, 0,0);
            updateneed = true;
         //   setPreferredHeight(elementheight);
            return;
        }
        if (!(showheadline) && (!(headline.getParent() == null))) {
            headlineContainer.removeChild(headline);
            updateneed = true;
       //     setPreferredHeight(elementheight);
        }

    }

    public void showPicture(boolean show){
        showimage = show;
        showPicture();
    }

    private void showPicture(){
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
        boolean pictureactive = false;
        // scaled lineheight
        float lineheight = new BitmapText(textField.getFont()).getLineHeight();
        lineheight = lineheight*(textField.getFontSize() / textField.getFont().getPreferredSize());

        // size of the headline
        float headlineheight =0;
        if (!(headline.getParent() == null)) headlineheight = Math.max(0, headline.getPreferredSize().getY());

        // height of middle part
        float middleheight =0;

        if (!(VPscenebox == null)) {
            if (!(VPscenebox.getParent() == null)) {
                middleheight = Math.max(VPscenebox.getPreferredSize().getY(),0);
                pictureactive = true;
            }
        }

        if (!(picture == null)){
            if (!(picture.getParent() == null)){
                middleheight = Math.max(picture.getPreferredSize().getY(),0);
                pictureactive = true;
            }
        }

        if ((elementheight-headlineheight-middleheight) <= lineheight) {
           middleheight = Math.max(elementheight -headlineheight-lineheight,0); // make scene smaller
           if (((elementheight-headlineheight-middleheight) <= lineheight) && (showheadline)) {
               showheadline =false; // disable headline
               headlineheight =0;
               showHeadline();
           }
           if (elementheight <lineheight) elementheight = lineheight; // change elementheight
       }
        // get number of lines
        float newnumberoflines = (elementheight-headlineheight-middleheight)/lineheight;
        newnumberoflines = (float) Math.floor(newnumberoflines);
        textField.setPreferredLineCount((int) newnumberoflines); // in element value will never be < 1
        textField.setmaxLinecount(textField.getPreferredLineCount());


        // get the textfield height and set the new height of scene if necessary
        float newtfheight = Math.max(lineheight,elementheight-headlineheight-middleheight);
        if (pictureactive) elementpictureheight =middleheight;

        // get the widths

        float sliderwidth=0;
        if (!(slidervert.getParent() == null)) {
            sliderwidth =slidervert.getPreferredSize().getX();
        }

        elementwidth = Math.max(elementwidth,sliderwidth); //make element bigger if needed
        float localelwidth;
        localelwidth = elementwidth - sliderwidth;

        if ((elementpicturewidth > localelwidth) && pictureactive) {
            elementpicturewidth = localelwidth; // change width of picture
        }

        float spacerwidth = 0;
        if (!(VPscenebox == null)) {
            if (!(VPscenebox.getParent() == null))  spacerwidth = (elementwidth/2) - (elementpicturewidth/2);
        }

        if (!(picture == null)){
            if (!(picture.getParent() == null)) spacerwidth = (elementwidth/2) - (elementpicturewidth/2);
        }


        // set all sizes

        headline.setPreferredWidth(localelwidth);
        textField.setPreferredWidth(localelwidth);
        tfContainer.setPreferredSize(new Vector3f(textField.getPreferredSize().x, newtfheight, 1f)); // Container kann abweichendes prefsize haben.
        if (!(VPscenebox == null))   VPscenebox.setPreferredSize(new Vector3f(elementpicturewidth, elementpictureheight, 1));
        if (!(picture == null))   picture.setPreferredSize(new Vector3f(elementpicturewidth, elementpictureheight, 1));

        Container tc = (Container) (( SpringGridLayout ) headlineContainer.getLayout()).getChild(1, 0);
        if (!(tc== null)) {
            ((Container) ((SpringGridLayout) tc.getLayout()).getChild(0, 0)).setPreferredSize(new Vector3f(spacerwidth,0,1));
        }

        updateneed = false;
    }

    private void updateslidermax() {
        baseIndex.setMaximum(Math.max(1, textField.getDocumentModel().getLineCount() - 1));
    }

    private void updateslidervalue() {
        lastcaratline = textField.getDocumentModel().getCaratLine();
        baseIndex.setValue(baseIndex.getMaximum() - lastcaratline);
        sliderref.update();
    }


    @Override
    public void updateLogicalState(float tpf) {
   //     System.out.println(tfContainer.getSize());
        super.updateLogicalState(tpf);

        if (updateneed) {
            textField.adjustText(0,true,true);
            updateallsizes();
        }

        if (tfmodelref.update()) {
            checkslider();
            if (!(textField.getDocumentModel().getCaratLine() == lastcaratline)) {
                updateslidermax();
                updateslidervalue();
            }
        }

        if (tfcaratref.update()) {
            if (!(textField.getDocumentModel().getCaratLine() == lastcaratline)) {
                updateslidervalue();
            }
        }
        if (sliderref.update()) {
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

    }



}
