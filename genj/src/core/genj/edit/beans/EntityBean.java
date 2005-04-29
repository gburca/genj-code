/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2002 Nils Meier <nils@meiers.net>
 *
 * This piece of code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package genj.edit.beans;

import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.Property;
import genj.gedcom.PropertyChange;
import genj.gedcom.TagPath;
import genj.gedcom.Transaction;
import genj.util.Registry;

import java.awt.BorderLayout;

import javax.swing.JLabel;

/**
 * A Proxy knows how to generate interaction components that the user
 * will use to change a property : ENTITY
 */
public class EntityBean extends PropertyBean {

  private Preview preview;
  private JLabel changed;

  /**
   * Finish editing a property through proxy (no changes here unless
   * hasChanged()==true since this will be called in all cases)
   */
  public void commit(Transaction tx) {
  }

  /**
   * Nothing to edit
   */  
  public boolean isEditable() {
    return false;
  }

  /**
   * Initialize
   */
  protected void initializeImpl() {
    preview = new Preview();
    changed = new JLabel();
    
    setLayout(new BorderLayout());
    add(BorderLayout.CENTER, preview);
    add(BorderLayout.SOUTH, changed);
  }
  
  /**
   * Set context to edit
   */
  protected void setContextImpl(Gedcom ged, Property prop, TagPath path, Registry reg) {

    Entity entity = (Entity)property;
    
    // show it
    preview.setEntity(entity);

    // add change date/time
    changed.setVisible(false);
    if (entity!=null) {
      PropertyChange change = entity.getLastChange();
      if (change!=null)
        changed.setText(resources.getString("entity.change", new String[] {change.getDateAsString(), change.getTimeAsString()} ));      
        changed.setVisible(true);
    }
    
    // Done
  }
  
} //ProxyEntity
