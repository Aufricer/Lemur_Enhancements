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

package com.simsilica.lemur.text;

import com.simsilica.lemur.TextField;
import com.simsilica.lemur.core.VersionedObject;
import com.simsilica.lemur.core.VersionedReference;

import java.util.List;

/**
 *  DocumentModel is a container for text that provides basic editing
 *  interaction as used by things like TextField.
 *
 *  @author    Paul Speed
 */
public interface DocumentModel extends VersionedObject<DocumentModel> {

    /**
     *  Deep clones this document model.
     */
    public DocumentModel clone();

    /**
     *  Replaces the text contained in this DocumentModel.
     */
    public void setText( String text );

    /**
     *  Returns the current text value contained in this DocumentModel adjusted with its current offset X+Y
     *  to get the full text check the getoffsetText() function
     */
    public String getText();


    /**
     *  Returns the string representing just the specified line of text.
     */
    public String getLine( int line );

    /**
     *  Returns the current number of lines in this document.
     */
    public int getLineCount();

    /**
     *  Returns the current 'carat' position.  The 'carat' is where
     *  new text characters will be inserted.  It's the current edit
     *  position.
     */
    public int getCarat();

    /**
     *  Returns the line number containing the current carat position.
     */

    public int getCaratLine();

    /**
     *  Returns the column number of the current carat position in the
     *  line returned by getCaratLine().
     */
    public int getCaratColumn();

    /**
     *  Moves the carat to the document's home position or the current line's
     *  home position depending on the specified 'currentLine' value.  If
     *  currentLine is true then the home position is just before the first
     *  character in the current line.  If the currentLine parameter is false then
     *  the home position is just before the first character in the whole
     *  document.
     */
    public int home( boolean currentLine );

    /**
     *  Moves the carat to the document's end position or the current line's
     *  end position depending on the specified 'currentLine' value.  If
     *  currentLine is true then the end position is just after the last
     *  character in the current line.  If the currentLine parameter is false then
     *  the end position is just after the last character in the whole
     *  document.
     */
    public int end( boolean currentLine );

    /**
     *  Moves the carat position to the previous line if there is one.  After
     *  this call, the new column position is implementation dependent.
     */
    public int up();

    /**
     *  Moves the carat position to the next line if there is one.  After this
     *  call, the new column position is implementation dependent.
     */
    public int down();

    /**
     *  Moves the carat one position to the left, potentially moving it to the
     *  previous line depending on the actual DocumentModel implementation.
     */
    public int left();

    /**
     *  Moves the carat one position to the right, potentially moving it to the
     *  next line depending on the actual DocumentModel implementation.
     */
    public int right();

    /**
     *  Inserts a new line at the current carat position.
     */
    public void insertNewLine();


    // deletes all selected text from the textfield, moves the carat accordingly and empty anchors
    // returns true if the carat position was inside an anchor
    // in case it is used to delete a selected character that is next to the carat it must
    // know the direction of the delete (back = true for backwards)
    // in all other cases just use false

    public boolean deleteSelectedText(boolean back);

    /**
     *  Deletes the character at the specified position.
     */
    public void deleteCharAt( int pos );

    /**
     *  Deletes the character immediately before the current carat position.
     *  This may move the carat to the previous line if the carat was previously
     *  at the beginning of a line.
     */
    public void backspace();

    /**
     *  Deletes the character immediately after the current carat position.
     */
    public void delete();

    // Finds the corresponding line and column of the given position in the (non offset) text

    void findPosition(int pos, int[] location);

    /**
     *  Inserts a character at the current carat position.
     */
    public void insert( char c );

    /**
     *  Bulk inserts a string of text.
     */
    public void insert( String text );

    /**
     *  Returns a VersionedReference that can be watched for changes to
     *  the carat position.
     */
    public VersionedReference<Integer> createCaratReference();

    // returns the full text of the document and avoid using the getoffsetText() directly
    String getfulltext();


    //  Returns the current text value contained in this DocumentModel adjusted with
    //  the specified offset, limited by the max visible lines
    // returns the full text if returnoffset is false

    String getoffsetText(int[] offset, boolean returnoffset);


    // set the maximum number of lines allowed, the value can be <= preferredlinecount
    // maxlines are needed with the scrollModes and is equal with preferredlinecount
    // its not advised but possible for the user to set maxlines manual

    public void setmaxlines(int ml);

    // updating the current carat position by setting an absolute or relative to current position value
    // the function will set line and column of the document model automatically
    // and is adjusting the offset if needed

    void updateCarat(boolean absolute_or_relative, int value, boolean increment);

    // get the current scrollMode of the document

    public TextField.ScrollMode getScrollMode();

    // set the current scrollMode of the document

    void setScrollMode(int scrollMode);

    // get the current offset in Y direction

    int getOffset_Y();

    // get if the document has an offset in Y direction

    boolean hasOffset_Y();

    // get the current offset in X direction

    int getOffset_X();

    // set the current offset in X direction

    void setOffset_X(int offset_X);

// set the current offset in Y direction (limited by documentmodel)

    void setOffset_Y(int newYoffsetY);


    // adds the given position of text to the anchors of textselect areas
    // position is checked against textlenght and shrinked or ignored automatically
    // existing and overlapping textselect areas will be merged

    void addTextselectArea(int startpos, int endpos);


    // remove the given position of text from existing anchors of textselect areas

    void removeTextselectArea(int startpos, int endpos);


    // reversing textselect in the given position
    // non selected areas will be selected, selected areas deselected

    void reverseSelect(int startpos, int endpos);

    // returns the current list with anchors of textselect areas
    List<int[]> getAnchors();

    // emptying the current list with anchors of textselect areas

    void emptyAnchors();

    // get the currently selected Text

    String getselectedText();

    // finds the value of potential Carat basing on the given location
    // returns null if the Carat would be out of text or location is null
    // reverse of findPosition

    Integer findCaratValue(int[] location);

    // returns a versioned Reference of the Anchor
    // can be used to check for updates of textselect areas


    VersionedReference<Integer> createAnchorReference();

}
