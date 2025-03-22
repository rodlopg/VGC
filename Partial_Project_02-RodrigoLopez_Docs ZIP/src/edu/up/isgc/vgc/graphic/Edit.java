package edu.up.isgc.vgc.graphic;

import edu.up.isgc.vgc.Component;

/**
 * Interface for editing media components (e.g., scaling, cutting, copying).
 */
public interface Edit {
    Component scale();
    Component cut();
    Component copy();
}