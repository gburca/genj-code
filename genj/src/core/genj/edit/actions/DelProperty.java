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
package genj.edit.actions;

import genj.edit.Images;
import genj.gedcom.GedcomException;
import genj.gedcom.Property;
import genj.view.ViewManager;

/**
 * PDelete - delete a property
 */  
public class DelProperty extends AbstractChange {
  
  /** the candidate to delete */
  private Property candidate;
  
  /**
   * Constructor
   */
  public DelProperty(Property property, ViewManager manager) {
    super(property.getGedcom(), Images.imgDelete, resources.getString("delete"), manager);
    candidate = property;
  }
  
  /**
   * @see genj.edit.EditViewFactory.Change#getConfirmMessage()
   */
  protected String getConfirmMessage() {
    // a veto?
    String veto = candidate.getDeleteVeto(); 
    // You are about to delete {0} of type {1} from {2}! Deleting this ...
    String msg = resources.getString("confirm.del.prop", new String[] { 
      candidate.getTag(), candidate.getEntity().getId() 
    });
    // result
    return veto==null ? msg : msg + '\n' + veto; 
  }
  
  /**
   * @see genj.edit.EditViewFactory.Change#change()
   */
  protected void change() throws GedcomException {
    candidate.getParent().delProperty(candidate);
  }
  
} //DelProperty

