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

import com.simsilica.lemur.core.VersionedObject;
import com.simsilica.lemur.core.VersionedReference;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;


/**
 *  A default implementation of the DocumentModel interface.
 *
 *  @author    Paul Speed
 */
public class DefaultDocumentModel implements DocumentModel, Cloneable {

    private long version;
    private List<StringBuilder> lines = new ArrayList<StringBuilder>();
    private String composite = null;
    private Carat carat = new Carat();
    private int line = 0;
    private int column = 0;

    // our textSelect Areas
    private Selector anchors = new Selector();
    private int maxlines = 1;
    private int scrollMode = 0;
    //offset_Y is calculated in the document model
    private int offset_Y = 0;
    private int offset_X = 0;


    public DefaultDocumentModel() {
        parseText("");
    }

    public DefaultDocumentModel(String text ) {
        parseText(text!=null?text:"");
    }

    @Override
    public DefaultDocumentModel clone() {
        try {
            DefaultDocumentModel result = (DefaultDocumentModel)super.clone();
            
            // Deep clone the lists
            result.lines = new ArrayList<StringBuilder>(lines.size());
            for( int i = 0; i < result.lines.size(); i++ ) {
                StringBuilder sb = lines.get(i);
                result.lines.set(i, new StringBuilder(sb));
            }
            
            result.carat = carat.clone();
            
            // And reset the version because it's ok for this document to start
            // over
            result.version = 0;
 
            return result;           
        } catch( CloneNotSupportedException e ) {
            throw new RuntimeException("Clone not supported", e);
        }
    }

    @Override
    public void setText( String text ) {
        parseText(text!=null?text:"");
    }


    @Override
    public String getText() {
        if( composite == null )
        //  changed this for the scrollmode
        createComposite(new int[] {offset_Y,offset_X});
        return composite;
    }


    @Override
    public String getLine( int line ) {
        return lines.get(line).toString();
    }

    @Override
    public int getLineCount() {
        return lines.size();
    }

    @Override
    public int getCarat() {
        return carat.get();
    }

    @Override
    public int getCaratLine() {
        return line;
    }

    @Override
    public int getCaratColumn() {
        return column;
    }
    // added an offset behaviour to most of the following functions
    @Override
    public int home( boolean currentLine ) {
        if( currentLine ) {
            carat.move(-column);
            column = 0;
        } else {
            carat.set(0);
            column = 0;
            line = 0;

            if ((scrollMode == 1|| scrollMode == 2) && !(offset_Y == 0)) {
                offset_Y =0;
                version++;
                composite = null;
            }

        }
        return carat.get();
    }

    @Override
    public int end( boolean currentLine ) {
        if( currentLine ) {
            StringBuilder row = lines.get(line);
            carat.move(row.length() - column);
            column = row.length();
        } else {
            // Find the end of the document
            carat.set(0);
            column = 0;
            line = 0;
            for( int i = 0; i < lines.size(); i++ ) {
                if( i > 0 ) {
                    carat.increment();
                }
                StringBuilder row = lines.get(i);
                carat.move(row.length());
                column = row.length();
            }
            line = lines.size() - 1;

            // If end line is higher then maxlines we need to set the Y-Offset
            if ((scrollMode == 1|| scrollMode == 2) && (maxlines < getCaratLine()+1)) {
                offset_Y = getLineCount()-maxlines;
                version++;
                composite = null;
            }
        }
        return carat.get();
    }

    @Override
    public int up() {
       if( line == 0 )
           return carat.get();

        if (getCaratLine()  - offset_Y <= 0 && !((scrollMode ==0)|| scrollMode ==3)) {
            // change the offset and inc versions for update loop
            offset_Y--;
            version++;
            carat.version++;
            composite = null; // Text need refresh
        }

        // Carat needs to lose the beginning of this line
        // Take it home
        carat.move(-column);

        // Take it to the end of the previous line
        line--;
        carat.decrement();


        if( column <= lines.get(line).length() ) {
            // Then we need to move the carat by the
            // rest of this line, too
            carat.move(-(lines.get(line).length() - column));
        } else {
            // Don't need to adjust the carat because it is already in the
            // right place.
            column = lines.get(line).length();
        }

        return carat.get();
    }

    @Override
    public int down() {
        if( line == lines.size() - 1 )
            return carat.get();

        // Take the carat to the end of this line
        int restOfLine = lines.get(line).length() - column;
        carat.move(restOfLine);

        // Take it to the beginning of the next line
        line++;
        carat.increment();

        // Then move it out as much as we can to fit the previous column
        column = Math.min(column, lines.get(line).length());
        carat.move(column);
        // same as new line
        if ((getCaratLine()+1 - offset_Y) > maxlines && !((scrollMode ==0)|| scrollMode ==3)) {
            offset_Y++;
            version++;
            composite =null;
        }

        return carat.get();
    }

    @Override
    public int left() {
        if( carat.get() == 0 )
            return 0;
        // moving the anchor or not
        carat.decrement();
        column--;
        if( column < 0 ) {
            line--;

            if( line < 0 ) {
                System.out.println( "How did this happen?  carat:" + carat );
            }

            // similiar to up, we have to check if line = 0
            if (getCaratLine()  - offset_Y <= 0 && !((scrollMode ==0)|| scrollMode ==3) && offset_Y > 0) {
                offset_Y--;
                version++;
                carat.version++;
                composite = null; // Text need refresh
            }


            column = lines.get(line).length();
        }

        return carat.get();
    }

    @Override
    public int right() {
        column++;
        carat.increment();
        if( column > lines.get(line).length() ) {
            if( line < lines.size() - 1 ) {
                line++;
                column = 0;
                // same as new line
                if ((getCaratLine()+1 - offset_Y) > maxlines && !((scrollMode ==0)|| scrollMode ==3)) {
                    offset_Y++;
                    version++;
                    composite =null;
                }

            } else {
                column--;
                carat.decrement();
            }
        }
        return carat.get();
    }

    @Override
    public void insertNewLine() {

        if( line == lines.size() - 1 && column == lines.get(line).length() ) {
            lines.add(new StringBuilder()); // Am Ende der Zeile
        } else {
            // Otherwise... we need to split the current line
            StringBuilder row = lines.get(line);
            StringBuilder next = new StringBuilder(row.substring(column));
            row.delete(column, row.length());
            lines.add(line+1, next);
        }
        // line numbers maybe restricted by max lines and scrollmode
        if (maxlines < lines.size() && ((scrollMode == 0) || scrollMode == 3)) {
            lines.get(maxlines-1).append(lines.remove(maxlines));
        } else {
            line++;
            column = 0;
            carat.increment();  // A new line is still a "character"
        }

        if ((getCaratLine()+1 - offset_Y) > maxlines && !((scrollMode ==0)|| scrollMode ==3)) {
            offset_Y++;
        }

        composite = null;
        version++;
    }

    @Override
    public void deleteCharAt( int pos ) {
        boolean del = false;
        // Some optimized paths
        if( pos == carat.get() - 1 ) {
            backspace();
            return;
        } else if( pos == carat.get() ) {
            delete();
            return;
        }

        int[] location = new int[2];
        findPosition(pos, location); // position of delete

        if( location[0] >= lines.size() )
            return; // nothing to delete

        StringBuilder row = lines.get(location[0]);
        if( location[1] == row.length() ) {
            if( location[0] < lines.size() - 1 ) {
                // Need to merge this line with the next
                row.append(lines.get(location[0]+1));
                lines.remove(location[0] + 1);
                // If a line is removed later we need to check if we have to adjust the offset
                del = true;
            } else {
                // Nothing to do and I don't know how the earlier
                // check failed.
                return;
            }
        } else {
            // Just delete the proper character
            row.deleteCharAt(location[1]);
        }

        // If the carat is after the delete position then
        // we need to adjust it... and the current line and column.
        if( carat.get() >= pos ) { // I believe here was an error in original
            carat.decrement();
            // reduce the offset
            if (del && (getCaratLine() + offset_Y) > maxlines && !((scrollMode ==0)|| scrollMode ==3)) {
                offset_Y--;
                }
            findPosition(carat.get(), location);
            line = location[0];
            column = location[1];
        }

        composite = null;
        version++;
    }

    @Override
    public void backspace() {

        if( carat.get() == 0 )
            return;

        if( column == 0 ) {
            if( line > 0 ) {
                // Need to merge this line with the previous
                column = lines.get(line-1).length();
                lines.get(line-1).append(lines.remove(line));
                carat.decrement();
                line--;

                if ((getCaratLine() + 1+ offset_Y) >= maxlines && !((scrollMode ==0)|| scrollMode ==3)) {
                    offset_Y--;
                }

            } else {
                // Nothing to do
                return;
            }
        } else {
            StringBuilder row = lines.get(line);
            row.deleteCharAt(column - 1);
            column--;
            carat.decrement();
        }
        composite = null;
        version++;
    }

    @Override
    public void delete() {
        StringBuilder row = lines.get(line);
        if( column == row.length() ) {
            if( line >= lines.size() - 1 )
                return;

            row.append(lines.remove(line+1));

            // delete can be called by delete char or by itself therefore we need to check the offset
            // if we find the carat position out of visible area we decrease it step by step
            if (getCaratLine()  - offset_Y >= maxlines && !((scrollMode ==0)|| scrollMode ==3) && offset_Y > 0) {
                while (getCaratLine() - offset_Y >= maxlines && offset_Y > 0) {
                    offset_Y--;
                }
            }

        } else {
            row.deleteCharAt(column);
        }
        composite = null;
        version++;
    }

    @Override
    public void findPosition( int pos, int[] location ) { // needed to make that public
        int index = 0;
        location[0] = 0;
        location[1] = 0;
        for( int r = 0; r < lines.size(); r++ ) {
            StringBuilder l = lines.get(r);
            if( pos - index <= l.length() ) {
                // Found the line
                location[0] = r;
                location[1] = pos - index;
                return;
            }

            // Else we need to advance
            index += l.length() + 1;
        }
        location[0] = lines.size();
        location[1] = 0;
    }

    @Override
    public void insert( char c ) {
        if( c < 32 )
            return;

        switch( c ) {
            default:
                // For now
                lines.get(line).insert(column, c);
                //carat++;
                carat.increment();
                column++;
                break;
        }
        composite = null;
        version++;
    }

    @Override
    public void insert( String text ) {
        for( int i = 0; i < text.length(); i++ ) {
            insert(text.charAt(i));
        }
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public DocumentModel getObject() {
        return this;
    }

    @Override
    public VersionedReference<DocumentModel> createReference() {
        return new VersionedReference<DocumentModel>(this);
    }

    @Override
    public VersionedReference<Integer> createCaratReference() {
        return carat.createReference();
    }




    protected void parseText( String text ) {
        composite = null;
        lines.clear();
        StringTokenizer st = new StringTokenizer(text, "\r\n");
        while( st.hasMoreTokens() ) {
            String token = st.nextToken();
            lines.add(new StringBuilder(token));
        }

        // Always at least one line
        if( lines.isEmpty() ) {
            lines.add(new StringBuilder());
        }

        end(false);
        version++;
        anchors.sversion++;

    }

    protected void createComposite() {
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < lines.size(); i++ ) {
            sb.append(lines.get(i));
            if( i < lines.size() - 1 ) {
                sb.append( "\n" );
            }
        }
        this.composite = sb.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[]";
    }


    // New stuff from here

    protected void createComposite(int[] offset) {

        // offset[0] = Y direction
        // offset[1] = X direction

        StringBuilder sb = new StringBuilder();

        int z = 0;
        for( int i = offset[0]; i < lines.size(); i++ ) {

            if (lines.get(i).length() >= offset[1]) {
                sb.append(lines.get(i).substring(offset[1]));
            }
            if( i < lines.size() - 1 ) {
                sb.append( "\n" );
            }
            z++;
            if (z >= maxlines) break; // max the lines allowed to show
        }

        this.composite = sb.toString();
    }

    @Override
    public void setmaxlines(int ml){
        this.maxlines = ml;
        // if maxlines are changed we reset the offset of Y to start at the beginning of the doc
        this.offset_Y =0;
    }

    @Override
    public String getfulltext(){
        int z[] = {0,0};
        return getoffsetText(z,false);
    }

    @Override
    public String getoffsetText(int[] offset,boolean returnoffset) {
        // gives us the text with the given offset in X- and Y direction and limited by maxLineCount
        // or returns the whole text
        if (returnoffset) {
            createComposite(offset);
        } else {
            createComposite();
            String Fulltext = composite;
            // need to set that null otherwise the resetText function in e.g. GUIUPDATE may take the wrong composite
            composite = null;
            return Fulltext;
        }
        return composite;
    }


    @Override
    public void updateCarat(boolean absolute_or_relative, int value, boolean increment){
        // the carat.move function does not reset line or column or care about offsets
        // in some cases we just need that functionality

        // move or set
        if (absolute_or_relative) {
            value = Math.max(value,0);
            carat.value = value;
        } else {
            carat.move(value);
        }

        if (increment) carat.version++;

        // set line and column
        int[] location = new int[2];
        findPosition(carat.value, location );
        line = location[0];
        column = location[1];

        // adjust our offset in case it was reset by e.g. textadjust() before and is
        // now out of visible area -> it will be set to the first visible line
        if (line < offset_Y) {
            offset_Y = line;
        }
    }

    @Override
    public int getScrollMode() {
        return scrollMode;
    }

    @Override
    public void setScrollMode(int scrollMode) {
        this.scrollMode = scrollMode;
    }

    @Override
    public int getOffset_Y () {
        return offset_Y;
    }

    @Override
    public boolean hasOffset_Y() {
         return (getOffset_Y() >0);
    }

    @Override
    public int getOffset_X() {
        return offset_X;
    }

    @Override
    public void setOffset_X(int offset_X) {
        // if offset_x changes our text need a refresh
        if (!(this.offset_X == offset_X)) {
            composite = null;
        }
        this.offset_X = offset_X;
    }

    @Override
    public void addTextselectArea(int startpos, int endpos) {
        anchors.addSelect(startpos, endpos);
    }

    @Override
    public void removeTextselectArea(int startpos, int endpos) {
        anchors.delSelect(startpos, endpos);
    }

    @Override
    public void reverseSelect(int startpos, int endpos) {

        // sorting the value pair and return if user data is invalid
        if (startpos == endpos) {
            return;
        } else {
            int tmp;
            if (startpos > endpos) {
                tmp = startpos;
                startpos = endpos;
                endpos = tmp;
            }
        }

        List<int[]> pos_Areas;
        int i;
        pos_Areas = anchors.getAreas_inSelectedAreas(startpos, endpos, false);
        anchors.addSelect(startpos, endpos);
        if (!(pos_Areas == null)) {
            int z = pos_Areas.size();
            for (i = 0; i < z; i++) {
                anchors.delSelect(pos_Areas.get(i));
            }
        }
    }

    @Override
    public boolean deleteSelectedText(boolean backspace ) {
        // we check the anchors, and get the not selected areas in our text
        if (anchors.getAncfield() == null) return true;

        int merker = carat.get();
        int j;
        boolean delnext = true;
        int o=0;

        // we need to recreate the text without offset
        // an alternative would be to use the getoffsetText method
        createComposite();

        List<int[]> delArea = anchors.getAreas_inSelectedAreas(0,composite.length(),true);


        if (delArea == null || delArea.size() == 0) {
            composite = null;
            version++;
            setText(composite);
            updateCarat(true,0,true);
            delnext = false;
        } else {
            String helper="";
            int [] valuepairs;

            for (j =0; j<delArea.size();j++) {
                valuepairs = delArea.get(j);
                helper = helper + composite.substring(Math.max(0,valuepairs[0]),Math.min(valuepairs[1],composite.length()));
            }

            if (backspace) o =1;

            for (j =0; j<anchors.getAncfield().size();j++) {
                valuepairs = anchors.getAncfield().get(j);
                if (valuepairs[1]<= carat.get()-o) {
                    merker -= (valuepairs[1] - valuepairs[0]);
                } else {
                    // adjustement in case carat is inside! the textselect area
                    if (valuepairs[0]<= carat.get()-o) {
                        delnext = false;
                        merker -= (carat.get()-valuepairs[0]);
                    }
                    break;
                }

            }
            setText(helper);
            updateCarat(true,merker,true);

        }
        anchors.emptySelector();
        return delnext;
    }

    @Override
    public List<int[]> getAnchors() {
        return anchors.getAncfield();
    }

    @Override
    public void emptyAnchors() {
        anchors.emptySelector();

    }

    @Override
    public String getselectedText(){
        List<int[]> l;
        int [] values;
        int i;
        String helper ="";

        l = getAnchors();
        createComposite();
        for (i = 0; i< l.size(); i++) {
            values = l.get(i);
            helper += composite.substring(values[0], values[1]);
        }

        return helper;
    }

    @Override
    public Integer findCaratValue(int[] location) {
        int i;
        int lng=0;

        if (location == null) return null;
        // check if the position is inside the current text
        if ((location[0] >= lines.size()) || location[0]<0) return null;
        if (location[1] > lines.get(location[0]).length() || location[1]<0) return null;

        lng = location[1];

        for (i = 0; i < location[0];i++) {
            lng += lines.get(i).length()+1;
        }
        return lng;
    }

    // end of new stuff

    private class Carat implements VersionedObject<Integer> {
        private int value;
        private long version;
        
        public Carat() {
        }
 
        public Carat clone() {
            Carat result = new Carat();
            result.value = value;
            // Don't need to set the version because it's a new object and
            // can start over.
            return result;
        }
 
        public final int get() {
            return value;
        }
 
        public final int set( int value ) {
            if( this.value == value ) {
                return value;
            }
            this.value = value;
            version++;
            return value;
        }
        
        public final int move( int amount ) {
            value += amount;
            version++;
            return value;
        }
        
        public final int increment() {
            value++;
            version++;
            return value;
        }
        
        public final int decrement() {
            value--;
            version++;
            return value;
        }

        @Override       
        public final long getVersion() {
            return version;
        } 

        @Override       
        public final Integer getObject() {
            return value;
        }

        @Override       
        public final VersionedReference<Integer> createReference() {
            return new VersionedReference<Integer>(this);
        }
 
        @Override       
        public final String toString() {
            return "Carat[" + value + "]";
        }
    }

    @Override
    public VersionedReference<Integer> createAnchorReference() {
        return anchors.createReference();
    }

    // new class
    private class Selector implements VersionedObject<Integer> {

        private List<int[]> ancfield = new  ArrayList<int[]>();
        private int value;
        private long sversion;

        private int markerleft;
        private int markerright;
        private boolean p1out = true;
        private boolean p2out = true;


        public Selector () {}


        public void emptySelector() {
            ancfield.clear();
            sversion++;
        }

        public List<int[]> getAncfield () {
            if (ancfield.size()==0) return null;
            return ancfield;
        }

        private void calcPositions (int[] valuepair) {
            int pos1;
            int pos2;
            int j;
            int[] tmpfield;

            markerleft =0;
            markerright =0;
            p1out = true;
            p2out = true;

            for (j = 0; j < ancfield.size(); j++) {
                tmpfield = ancfield.get(j);
                pos1 = tmpfield[0];
                pos2 = tmpfield[1];

                if (valuepair[0] < pos1) {
                    p1out = true;
                    break;
                }
                if (valuepair[0] >= pos1 && valuepair[0] <= pos2) {
                    // in this anchor
                    p1out = false;
                    j++;
                    break;
                }
            }
            if (j == ancfield.size() && p1out) {
                j++;
            }

            markerleft = j;


            for (j = Math.max(markerleft - 1, 0); j < ancfield.size(); j++) {
                tmpfield = ancfield.get(j);
                pos1 = tmpfield[0];
                pos2 = tmpfield[1];

                if (valuepair[1] < pos1) {
                    p2out = true;
                    break;
                }
                if (valuepair[1] >= pos1 && valuepair[1] <= pos2) {
                    // in this anchor
                    p2out = false;
                    j++;
                    break;
                }
            }
            if (j == ancfield.size() && p2out) {
                j++;
            }
            markerright = j;

        }

        public void addSelect(int[] valuepair) {
            int[] tmpfield = {0,0};
            int pos1;
            int i;

            // sorting the value pair
            if (valuepair[0] == valuepair[1] || valuepair[0] < 0 || valuepair[0] < 0) {
                return;
            } else {
                if (valuepair[0] > valuepair[1]) {
                    pos1 = valuepair[0];
                    valuepair[0] = valuepair[1];
                    valuepair[1] = pos1;
                }
            }

            tmpfield[0] = 0;
            tmpfield[1] = 0;
            // adjusting to textlength or ignore
            if (valuepair[0] > getoffsetText(tmpfield,false).length()) return;
            if (valuepair[1] > getoffsetText(tmpfield,false).length()) {
                valuepair[1] = getoffsetText(tmpfield,false).length();
                System.out.println("Selectors size adjusted to textlenght");
            }


            // if we dont have an anchor yet we just add the value pair
            if (ancfield.size() == 0) {
                ancfield.add(valuepair);
                sversion++;
                return;
            }


            // find position of our valuepair
            calcPositions(valuepair);

            // both positions outside a textSelect area
            // = valuefield before, between or after (same) anchors
            if ((markerleft == markerright) && (p1out && p2out)) {
                if (markerleft > ancfield.size()) markerleft--;
                ancfield.add(markerleft, valuepair);
                sversion++;
                return;
            }

            // both positions outside an textSelect area, that is not the same
            if (!(markerleft == markerright) && (p1out && p2out)) {
                int x[] = new int[]{0, 0};
                x[0] = valuepair[0];
                x[1] = valuepair[1];
                ancfield.add(markerleft, x);

                if (markerright > ancfield.size() - 1) markerright--;

                for (i = 0; i < markerright - markerleft; i++) {
                    ancfield.remove(markerleft+1);
                }
                sversion++;
                return;
            }

            // both positions inside the same textSelect area
            if ((markerleft == markerright) && (!p1out && !p2out)) {
                return;
            }

            // both positions inside a textSelect area, that is not the same
            if (!(markerleft == markerright) && (!p1out && !p2out)) {
                tmpfield = ancfield.get(markerleft - 1);
                int x[] = new int[]{0, 0};
                x[0] = tmpfield[0];
                tmpfield = ancfield.get(markerright - 1);
                x[1] = tmpfield[1];

                if ((markerleft == 1) && (markerright == ancfield.size())) {
                    ancfield.clear();
                    ancfield.add(x);
                } else {
                    ancfield.add(markerleft - 1, x);
                    for (i = 0; i < markerright - markerleft + 1; i++) {
                        ancfield.remove(markerleft);
                    }
                }
                sversion++;
                return;
            }

            // pos1 in a textSelect area pos2 outside
            if (!p1out && p2out) {
                tmpfield = ancfield.get(markerleft - 1);
                int x[] = new int[]{0, 0};
                x[0] = tmpfield[0];
                x[1] = valuepair[1];
                ancfield.add(markerleft - 1, x);

                if (markerright > ancfield.size() - 1) markerright--;

                for (i = 0; i < markerright - markerleft + 1; i++) {
                    ancfield.remove(markerleft);
                }
                sversion++;
                return;
            }

            // pos1 outside a textSelect area pos2 in
            if (p1out && !p2out) {
                tmpfield = ancfield.get(markerright - 1);
                int x[] = new int[]{0, 0};
                x[0] = valuepair[0];
                x[1] = tmpfield[1];
                ancfield.add(markerleft, x);

                for (i = 0; i < markerright - markerleft; i++) {
                    ancfield.remove(markerleft+1);
                }
                sversion++;
                return;
            }
        }

        public void addSelect(int pos_A, int pos_B) {
            int [] i = new int[] {pos_A,pos_B};
            addSelect(i);
        }

        public void delSelect(int[] valuepair) {
            int[] tmpfield ={0,0};
            int pos1;
            int del =0;
            int i;

            // sorting the value pair and return if user data is invalid
            if (valuepair[0] == valuepair[1] || valuepair[0] < 0 || valuepair[0] < 0 ||ancfield.size() == 0) {
                return;
            } else {
                if (valuepair[0] > valuepair[1]) {
                    pos1 = valuepair[0];
                    valuepair[0] = valuepair[1];
                    valuepair[1] = pos1;
                }
            }

            // adjusting to textlength or ignore
            tmpfield[0] = 0;
            tmpfield[1] = 0;
            if (valuepair[0] > getoffsetText(tmpfield,false).length()) return;
            if (valuepair[1] > getoffsetText(tmpfield,false).length()) {
                valuepair[1] = getoffsetText(tmpfield,false).length();
            }

            // find position of valuepair
            calcPositions(valuepair);

            // both positions outside the same textSelect area
            // = valuefield before, between or after (same) anchors
            if ((markerleft == markerright) && (p1out && p2out)) {
                return;
            }

            // both positions outside a textSelect area with anchors in between
            if (!(markerleft == markerright) && (p1out && p2out)) {
                if (markerright > ancfield.size() - 1) markerright--;
                for (i = 0; i < markerright - markerleft; i++) {
                    ancfield.remove(markerleft);
                }
                sversion++;
                return;
            }

            // both positions inside the same textSelect area
            if ((markerleft == markerright) && (!p1out && !p2out)) {
                tmpfield = ancfield.get(markerleft - 1);
                // all anchor
                if (tmpfield[0] == valuepair[0] && tmpfield[1] == valuepair[1]) {
                        ancfield.remove(markerleft-1);
                        sversion++;
                        return;
                }
                // remove left part of anchor
                if (valuepair[0] == tmpfield[0]) {
                    tmpfield[0] = valuepair[1];
                    sversion++;
                    return;
                }
                // remove right part of anchor
                if (valuepair[1] == tmpfield[1]) {
                    tmpfield[1] = valuepair[0];
                    sversion++;
                    return;
                }
                // cut anchor into 2 parts
                int x[] = new int[]{0, 0};
                x[0] = valuepair[1];
                x[1] = tmpfield[1];
                tmpfield[1] = valuepair[0];
                ancfield.add(markerleft,x);
                sversion++;
                return;
            }

            // both positions inside a textSelect area, that is not the same
            if (!(markerleft == markerright) && (!p1out && !p2out)) {
               del = 0;
                // delete anchors in between
                for (i = 0; i < markerright - markerleft-1; i++) {
                    ancfield.remove(markerleft);
                    del++;
                }
                // addjust or delete anchors left and right the position
                tmpfield = ancfield.get(markerleft - 1);
                if (valuepair[0] == tmpfield[0]) {
                    ancfield.remove(markerleft - 1);
                    del++;
                } else {
                    tmpfield[1] = valuepair[0];
                }

                tmpfield = ancfield.get(markerright-1-del);
                if (valuepair[1] == tmpfield[1]) {
                    ancfield.remove(markerleft - 1);
                    sversion++;
                    return;
                } else {
                    tmpfield[0] = valuepair[1];
                    sversion++;
                    return;
                }
            }

            // pos1 in a textSelect area pos2 outside
            if (!p1out && p2out) {
                // delete anchors in between
                if (markerright > ancfield.size()) markerright--;
                for (i = 0; i < markerright - markerleft; i++) {
                    ancfield.remove(markerleft);
                }
                // adjust or delete anchor on left border
                tmpfield = ancfield.get(markerleft - 1);
                if (valuepair[0] == tmpfield[0]) {
                    ancfield.remove(markerleft - 1);
                } else {
                    tmpfield[1] = valuepair[0];
                }
                sversion++;
                return;
            }

            // pos1 outside a textSelect area pos2 in
            if (p1out && !p2out) {
                del =0;
                for (i = 0; i < markerright -1- markerleft; i++) {
                    ancfield.remove(markerleft);
                    del++;
                }
                // adjust or delete anchor on right border
                tmpfield = ancfield.get(markerright - 1-del);
                if (valuepair[1] == tmpfield[1]) {
                    ancfield.remove(markerright - 1-del);
                } else {
                    tmpfield[0] = valuepair[1];
                }
                sversion++;
                return;
            }
        }

        public void delSelect(int pos_A, int pos_B) {
            int [] i = new int[] {pos_A,pos_B};
            delSelect(i);
        }

        // returns a list with anchors of selected or unselected areas between the given positions
        public List<int[]> getAreas_inSelectedAreas(int pos_A, int pos_B, boolean negativeAreas){
            int i;
            List<int[]> positives = new  ArrayList<int[]>();
            List<int[]> negatives = new  ArrayList<int[]>();
            int[] tmp;
            int[] values ={0,0};
            int fortschritt;

            if (ancfield.size() == 0) {
                if (!negativeAreas) return null;
                tmp = new int[]{pos_A,pos_B};
                negatives.add(tmp);
                return negatives;
            }

            // calc the positions
            calcPositions(new int[] {pos_A,pos_B});

            // area is after last anchor
            if (p1out  && ancfield.size() < markerleft) {
                values[0] = pos_A;
                values[1] = pos_B;
                negatives.add(values);
                if (negativeAreas) return  negatives;
                return null;
            }

            // posA is outside an anchor
            if (p1out) {
                tmp = ancfield.get(markerleft);
                values[0] = pos_A;
                values[1] = Math.min(pos_B,tmp[0]);
                negatives.add(values);
                // posB is before next anchor
                if (values[1] == pos_B) {
                    if (negativeAreas) return  negatives;
                    return null;
                }
                fortschritt= values[1];
            } else { // pos1 inside anchor
                fortschritt = pos_A;
                markerleft--;
            }

            // get the areas between known anchors
            for (i = markerleft; i<Math.min(markerright,ancfield.size());i++) {
                tmp = ancfield.get(i);
                if (i > markerleft) {
                    values = new int[] {0,0};
                    values[0] = fortschritt;
                    values[1] = tmp[0];
                    negatives.add(values);
                    fortschritt = values[1];
                }
                values = new int[] {0,0};
                values[0] = fortschritt;
                values[1] = tmp[1];
                fortschritt = tmp[1];
                if (!(values[0]==values[1]))  positives.add(values);
            }

            // add the end of the position
            if (p2out) {
                // after last anchor or between anchors
                values = new int[] {0,0};
                values[0] = fortschritt;
                values[1] = pos_B;
                negatives.add(values);
            } else { // inside an anchor
                tmp =   positives.get(positives.size()-1);
                if (tmp[0] == pos_B) {
                    positives.remove(positives.size()-1);
                } else {
                    tmp[1] = pos_B;
                }
            }

            if (negativeAreas) {
                return  negatives;
            } else {
                return positives;
            }
        }

        @Override
        public long getVersion() {
            return sversion;
        }

        @Override
        public Integer getObject() {
            return value;
        }

        @Override
        public VersionedReference<Integer> createReference() {
            return new VersionedReference<Integer>(this);
        }
    }

 }
