import com.jme3.font.*;
import com.jme3.font.BitmapText;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.*;
import com.simsilica.lemur.component.BorderLayout;
import com.simsilica.lemur.core.GuiControl;
import com.simsilica.lemur.core.VersionedReference;
import com.simsilica.lemur.event.MouseEventControl;
import com.simsilica.lemur.style.*;

public class ReadField extends Panel {

    public com.simsilica.lemur.TextField textField;
    public com.simsilica.lemur.TextField headline;
    public Container tfContainer;

    private float elementwidth;
    private float elementheight;


    private static final String ELEMENT_ID = "readField";
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


    public ReadField(String text, String headline) {
        this(true, new ElementId(ELEMENT_ID), null, headline, text);
    }

    public ReadField(String text, String headline, String style) {
        this(true, new ElementId(ELEMENT_ID), style, headline, text);
    }

    public ReadField(String text, String headline, String style, boolean showheadline) {
        this(true, new ElementId(ELEMENT_ID), style, headline, text);
        Showheadline(showheadline);
    }

    public ReadField(String text, String headline, boolean showheadline) {
        this(true, new ElementId(ELEMENT_ID), null, headline, text);
        Showheadline(showheadline);
    }


    protected ReadField(boolean applyStyles, ElementId elementId, String style, String headlinetxt, String text) {
        // we dont apply styles for the super type but then we do it at the end
        super(false, elementId, style);


        this.layout = new BorderLayout();
        getControl(GuiControl.class).setLayout(layout);


        // create textfield and reference, attach mouselistener
        tfContainer = new Container(elementId.child(CONTAINER_ID), style);
        //   tfContainer.setBackground(new QuadBackgroundComponent(ColorRGBA.Red));
        textField = new com.simsilica.lemur.TextField(text, elementId.child(TextField_ID), style);
        tfmodelref = textField.getDocumentModel().createReference();
        tfcaratref = textField.getDocumentModel().createCaratReference();


        tfContainer.addChild(textField);
        layout.addChild(tfContainer, BorderLayout.Position.Center);
        //  textField = new TextField(text,style);
        //  layout.addChild(textField, BorderLayout.Position.Center);

        selector = new Textfieldselectors();
        MouseEventControl.addListenersToSpatial(textField, selector);


        // create slider and reference
        baseIndex = new DefaultRangedValueModel();
        sliderref = baseIndex.createReference();
        baseIndex.setMaximum(10);
        baseIndex.setValue(4);
        slidervert = new Slider(baseIndex, Axis.Y, elementId.child(SLIDER_ID), this.getStyle());


        // creation and preparation of headline, note it must be set active elsewhere
        ElementId id = new ElementId(Headline_ID).child(elementId.child(TextField_ID));
        headline = new com.simsilica.lemur.TextField("", id, style);
        //
        headline.getActionMap().clear();
        headline.setPreferredCursorWidth(0f);
        getHeadline().setText(headlinetxt);

        elementheight = tfContainer.getPreferredSize().getY();
        elementwidth = tfContainer.getPreferredSize().getX();

        if (applyStyles) {
            Styles styles = GuiGlobals.getInstance().getStyles();
            meid = this.getElementId();
            styles.applyStyles(this, getElementId(), style);
        }


    }


    public com.simsilica.lemur.TextField getTextfield() {
        return textField;
    }

    public com.simsilica.lemur.TextField getHeadline() {
        return headline;
    }

    public float getElementwidth() {
        return elementwidth;
    }

    public float getElementheight() {
        return elementheight;
    }

    @StyleDefaults(ELEMENT_ID)
    public static void initializeDefaultStyles(Styles styles, Attributes attrs) {
        // most attributes come already from the style appstate - if that one is not available
        // some standard settings can be made here
        styles.getSelector(meid, null).set("setReadfieldPreferredWidth", 50, false);
        styles.getSelector(meid, null).set("setheadlineHAlignment", HAlignment.Center, false);
        //    styles.getSelector(meid, null).set("setPreferredHeight", 50,false);

        //   styles.getSelector(meid, null).set("Showheadline", false,false); Does not work to set it via style

    }


    @StyleAttribute(value = "setheadlineHAlignment", lookupDefault = false)
    public void setheadlineHAlignment(HAlignment hAlignment) {
        headline.setTextHAlignment(hAlignment);
    }

    @StyleAttribute(value = "setPreferredHeight", lookupDefault = false)
    public void setPreferredHeight(float height) {
        // get line height first, then set the new line number to textfield
        float lh = new BitmapText(textField.getFont()).getLineHeight();
        float scale = textField.getFontSize() / textField.getFont().getPreferredSize();
        // we check if font is scaled
        lh = lh * scale;
        float headlinesize;

        if (headline.getParent() == null) {
            headlinesize = 0;
        } else {
            headlinesize = Math.max(0, headline.getPreferredSize().getY());
        }

        // take the new height - height of headline and then calculate number of lines
        float newlines = (float) Math.floor(height - headlinesize) / lh;
        textField.setPreferredLineCount((int) newlines); // in element value will never be < 1
        textField.setmaxLinecount(textField.getPreferredLineCount());

        // set the textfield container to right size  - note that textfield itself may be smaller then tfcontainer

        float newsize = Math.max(lh, height - headlinesize);
        tfContainer.setPreferredSize(new Vector3f(tfContainer.getPreferredSize().x, newsize, 1f));
        elementheight = headlinesize + newsize; //maybe not necessary

        // checkslider
    }

    @StyleAttribute(value = "setReadfieldPreferredWidth", lookupDefault = false)
    public void setReadfieldPreferredWidth(float width) {
        // Element will get the probber width, no matter if slider is on or not

        elementwidth = Math.max(0, width);
        float locelwidth;
        if (slidervert.getParent() == null) {
            locelwidth = elementwidth;
        } else {
            locelwidth = Math.max(0, Math.max(width - slidervert.getPreferredSize().getX(), slidervert.getPreferredSize().getX()));
        }
        headline.setPreferredWidth(locelwidth);
        textField.setPreferredWidth(locelwidth);
        checkslider();
    }

    public void Showheadline(Boolean show) {
        if ((show) && (headline.getParent() == null)) {
            layout.addChild(headline, BorderLayout.Position.North);
            setPreferredHeight(elementheight);
            return;
        }
        if (!(show) && (!(headline.getParent() == null))) {
            this.layout.removeChild(headline);
            setPreferredHeight(elementheight);
        }
    }

    private void checkslider() {

        if (textField.getDocumentModel().getLineCount() < textField.getPreferredLineCount()) {
            // kein slider notwendig
            if (!(slidervert.getParent() == null)) {
                layout.removeChild(slidervert);
                setReadfieldPreferredWidth(elementwidth);
            }
        } else {
            // slider notwendig
            // ermittle und setze neu Sliderwerte
            updateslidermax();
            updateslidervalue();
            //    baseIndex.setValue(0);
            if (slidervert.getParent() == null) { // add slider, wenn notwendig
                layout.addChild(slidervert, BorderLayout.Position.East);
                setReadfieldPreferredWidth(elementwidth);
            }

        }
    }

    private void updateslidermax() {
        baseIndex.setMaximum(Math.max(1, textField.getDocumentModel().getLineCount() - 1));

    }

    private void updateslidervalue() {
        lastcaratline = textField.getDocumentModel().getCaratLine();
        baseIndex.setValue(baseIndex.getMaximum() - lastcaratline);
        sliderref.update();
    }


    int o = 0;

    @Override
    public void updateLogicalState(float tpf) {
        super.updateLogicalState(tpf);


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
