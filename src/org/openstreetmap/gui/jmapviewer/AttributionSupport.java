// License: GPL. For details, see Readme.txt file.
package org.openstreetmap.gui.jmapviewer;

import static org.openstreetmap.gui.jmapviewer.FeatureAdapter.tr;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.awt.image.ImageObserver;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.gui.jmapviewer.interfaces.Attributed;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;

public class AttributionSupport {
    public static final Font ATTR_FONT = new Font("Arial", Font.PLAIN, 10);
    public static final Font ATTR_LINK_FONT;
    private static final int PADDING = 5;

    private Attributed source;

    private Image attrImage;
    private String attrTermsText;
    private String attrTermsUrl;

    protected Rectangle attrTextBounds;
    protected Rectangle attrToUBounds;
    protected Rectangle attrImageBounds;

    static {
        Map<TextAttribute, Integer> aUnderline = new HashMap<>();
        aUnderline.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        ATTR_LINK_FONT = ATTR_FONT.deriveFont(aUnderline);
    }

    public void initialize(Attributed source) {
        this.source = source;
        boolean requireAttr = source.requiresAttribution();
        if (requireAttr) {
            attrImage = source.getAttributionImage();
            attrTermsText = source.getTermsOfUseText();
            attrTermsUrl = source.getTermsOfUseURL();
            if (attrTermsUrl != null && attrTermsText == null) {
                attrTermsText = tr("Background Terms of Use");
            }
        } else {
            attrImage = null;
            attrTermsUrl = null;
        }
    }

    public void paintAttribution(Graphics g, int width, int height, ICoordinate topLeft, ICoordinate bottomRight,
            int zoom, ImageObserver observer) {
        if (source == null || !source.requiresAttribution()) {
            attrToUBounds = null;
            attrImageBounds = null;
            attrTextBounds = null;
            return;
        }

        // Draw attribution
        Font font = g.getFont();
        g.setFont(ATTR_LINK_FONT);

        // Draw terms of use text (bottom left corner)
        final int fontDescent = g.getFontMetrics().getDescent();
        int termsTextHeight = 0;
        int textY = height - 1 - PADDING; // 1px offset to compensate for the text shadow

        if (attrTermsText != null) {
            Rectangle2D termsStringBounds = g.getFontMetrics().getStringBounds(attrTermsText, g);
            int textRealHeight = (int) termsStringBounds.getHeight();
            termsTextHeight = textRealHeight;
            int termsTextWidth = (int) termsStringBounds.getWidth();
            attrToUBounds = new Rectangle(PADDING, textY - termsTextHeight + fontDescent, termsTextWidth, textRealHeight);
            drawShadedText(g, attrTermsText, PADDING, textY);
        } else {
            attrToUBounds = null;
        }

        // Draw attribution logo (on top of the terms of use text)
        if (attrImage != null) {
            int imgWidth = attrImage.getWidth(observer);
            int imgHeight = attrImage.getHeight(observer);
            int y = textY - imgHeight - termsTextHeight;
            attrImageBounds = new Rectangle(PADDING, y, imgWidth, imgHeight);
            g.drawImage(attrImage, PADDING, y, null);
        } else {
            attrImageBounds = null;
        }

        // Draw attribution (bottom right corner)
        g.setFont(ATTR_FONT);
        String attributionText = source.getAttributionText(zoom, topLeft, bottomRight);
        if (attributionText == null) {
            // In case the attribution text has been forgotten, display URL
            attributionText = source.getAttributionLinkURL();
        }
        if (attributionText != null) {
            Rectangle2D stringBounds = g.getFontMetrics().getStringBounds(attributionText, g);
            int textWidth = (int) stringBounds.getWidth();
            int textHeight = (int) stringBounds.getHeight();
            int x = width - textWidth - PADDING;
            drawShadedText(g, attributionText, x, textY);
            attrTextBounds = new Rectangle(x, textY - textHeight + fontDescent, textWidth, textHeight);
        } else {
            attrTextBounds = null;
        }

        g.setFont(font);
    }

    /**
     * Draws a string with 1px shadow.
     * @param g the graphics
     * @param text the string to be drawn
     * @param x the x coordinate
     * @param y the y coordinate
     */
    private static void drawShadedText(Graphics g, String text, int x, int y) {
        g.setColor(Color.black);
        g.drawString(text, x + 1, y + 1);
        g.setColor(Color.white);
        g.drawString(text, x, y);
    }

    public boolean handleAttributionCursor(Point p) {
        if (p == null) return false;

        return (attrTextBounds != null && attrTextBounds.contains(p))
            || (attrImageBounds != null && attrImageBounds.contains(p))
            || (attrToUBounds != null && attrToUBounds.contains(p));
    }

    public boolean handleAttribution(Point p, boolean click) {
        if (source == null || !source.requiresAttribution())
            return false;

        if (attrTextBounds != null && attrTextBounds.contains(p)) {
            String attributionURL = source.getAttributionLinkURL();
            if (attributionURL != null) {
                if (click) {
                    FeatureAdapter.openLink(attributionURL);
                }
                return true;
            }
        } else if (attrImageBounds != null && attrImageBounds.contains(p)) {
            String attributionImageURL = source.getAttributionImageURL();
            if (attributionImageURL != null) {
                if (click) {
                    FeatureAdapter.openLink(source.getAttributionImageURL());
                }
                return true;
            }
        } else if (attrToUBounds != null && attrToUBounds.contains(p)) {
            String termsOfUseURL = source.getTermsOfUseURL();
            if (termsOfUseURL != null) {
                if (click) {
                    FeatureAdapter.openLink(termsOfUseURL);
                }
                return true;
            }
        }
        return false;
    }

}
