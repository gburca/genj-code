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
package genj.gedcom;

/**
 * Gedcom Property : Birth
 */
public class PropertyBirth extends PropertyEvent {

  /**
   * Constructor of birth Gedcom-line
   */
  public PropertyBirth(String tag) {
    super(tag);
  }

  /**
   * Constructor of birth Gedcom-line
   */
  public PropertyBirth(String tag, String value) {
    super(tag, value);
  }

  /**
   * Returns the logical name of the proxy-object which knows this object
   */
  public String getProxy() {
    return "Birth";
  }

  /**
   * Returns the logical name of the proxy-object which knows this object
   */
  public static String getProxy(TagPath path) {
    return "Birth";
  }
}
