/**
 * GraphJ
 * 
 * Copyright (C) 2002 Nils Meier
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */
package gj.shell;

import gj.awt.geom.Dimension2D;
import gj.awt.geom.Path;
import gj.awt.geom.ShapeHelper;
import gj.layout.Layout;
import gj.layout.LayoutRenderer;
import gj.model.Arc;
import gj.model.MutableGraph;
import gj.model.Node;
import gj.shell.swing.SwingHelper;
import gj.shell.swing.UnifiedAction;
import gj.shell.util.ReflectHelper;
import gj.util.ArcHelper;
import gj.util.ArcIterator;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Method;
import java.util.Iterator;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

/**
 * Displaying a Graph with user input
 */
public class GraphWidget extends JPanel {
  
  /** the Shapes we know */
  protected Shape[] shapes = new Shape[] {
    new Rectangle2D.Double(-16,-16,32,32),
    ShapeHelper.createShape(23D,19D,1,1,new double[]{
      0,6,4,1,18,11,1,29,1,1,34,30,1,47,27,1,31,44,1,20,24,1,6,38,1,2,21,1,14,20,1,6,4
    }),
    ShapeHelper.createShape(15D,15D,1,1,new double[] {
      0,10,0,
      1,20,0,
       2,30,0,30,10, // tr : quad
      1,30,20,
       2,20,20,20,30, // br : quad
      1,10,30,
       3,20,20,10,10,0,20, // bl : cubic
      1,0,10,
       3,-10,0,0,-10,10,0 // tl : cubic
    })
  };
  
  /** the graph we're displaying */
  private MutableGraph graph;
  
  /** the size of the graph */
  private Rectangle graphBounds;
  
  /** the content for the graph */
  private Content content = new Content();
  
  /** the renderer we're using */
  private GraphRenderer graphRenderer = new GraphRenderer() {
    protected Color getColor(Node node) {
      return node==selection?Color.blue:Color.black;
    }
  };
  
  /** more renderers */
  private LayoutRenderer layoutRenderer = null;
  
  /** the lastly selected element */
  private Node selection = null;
  
  /** whether quicknode is enabled */
  private boolean quickNode = false;
  
  /** whether antialiasing is on */
  private boolean isAntialiasing = true;
  
  /** our mouse analyzers */
  private DnD
    dndNoOp = new DnDIdle(),
    dndMoveNode = new DnDMoveNode(),
    dndCreateArc = new DnDCreateArc(),
    dndResizeNode = new DnDResizeNode(),
    dndCurrent = null;
    
  /** a layout we know about */
  private Layout currentLayout;
    
  /**
   * Constructor
   */
  public GraphWidget() {
    
    // Layout
    setLayout(new BorderLayout());
    add(new JScrollPane(content), BorderLayout.CENTER);
    
    // Done
  }
  
  /**
   * Accessor - Graph
   */
  public void setGraph(MutableGraph setGraph, Rectangle setBounds) {
    
    // cleanup data
    graph = setGraph;
    graphBounds = setBounds;
    selection = null;
    
    // make sure that's reflected
    revalidate();
    
    // start fresh with DnD
    dndNoOp.start(null);
    
    // done
  }
  
  /**
   * Accessor - Antialiasing enabled or not
   */
  public boolean isAntialiasing() {
    return isAntialiasing;
  }

  /**
   * Accessor - Antialiasing enabled or not
   */
  public void setAntialiasing(boolean set) {
    isAntialiasing=set;
    repaint();
  }
  
  /**
   * @see JComponent#revalidate()
   */
  public void revalidate() {
    if (content!=null) content.revalidate();
    super.revalidate();
    repaint();
  }
  
  /** 
   * Accessor - the current layout
   */
  public void setCurrentLayout(Layout layout) {
    currentLayout = layout;
    layoutRenderer = getRenderer(layout);
    repaint();
  }
  
  /**
   * Helper that calculates a Renderer for given Layout
   */
  private LayoutRenderer getRenderer(Layout instance) {
    if (instance==null) return null;
    try {
      return (LayoutRenderer)ReflectHelper.getInstance(instance.getClass().getName()+"Renderer", LayoutRenderer.class);
    } catch (Throwable t) {
      return null;
    }
  }
  
  /**
   * Returns the popup for nodes
   */
  private JPopupMenu getNodePopupMenu(Point where) {

    // Create the popups
    JPopupMenu result = new JPopupMenu();
    result.add(new ActionResizeNode());
    result.add(new ActionDeleteNode());
    result.add(new ActionCreateArc(where));
    result.add(new ActionSetNodeContent());
    JMenu mShape = new JMenu("Set Shape");
    for (int i=0;i<shapes.length;i++)
      mShape.add(new ShapeMenuItemWidget(shapes[i], new ActionSetNodeShape(shapes[i])));
    result.add(mShape);

    // do we have a a layout with input for the node's menu?
    if (currentLayout==null) 
      return result;
      
    // continue with menu's submenu
    JMenu mLayout = new JMenu();
    
    // collect public setters(Node)
    Method[] actions = ReflectHelper.getMethods(currentLayout, "", new Class[]{ Node.class});
    if (actions.length>0) {
      // add add actions
      for (int a=0; a<actions.length; a++) {
        mLayout.add(new ActionNodeLayoutMethod(actions[a]));
      }
      // visible now
      mLayout.setText(currentLayout.toString());
      result.add(mLayout);
    }

    // done
    return result;    
  }
  
  /**
   * Returns the popup for canvas
   */
  private JPopupMenu getCanvasPopupMenu(Point pos) {
    
    JPopupMenu result = new JPopupMenu();
    result.add(new ActionCreateNode(pos));
    result.add(SwingHelper.getCheckBoxMenuItem(new ActionToggleQuickNode()));
    return result;
  }

  /**
   * Tries to find an element at given position
   */
  private Node getElement(Point screen) {
    Point at = screen2model(screen);
    // try to find a node
    Iterator nodes = graph.getNodes().iterator();
    while (nodes.hasNext()) {
      Node node = (Node)nodes.next();
      Point2D pos = node.getPosition();
      if (node.getShape().contains(at.x-pos.getX(),at.y-pos.getY())) {
        return node;
      }
    }
    // try to find an arc
    // TODO
    // nothing found
    return null;    
  }
    
  /**
   *  Creates a node at given position
   */
  private void createNode(Point screen, Object object) {

    // let the graph do it
    graph.addNode(screen2model(screen), shapes[0], object);
    
    // show it
    repaint();
  }
  
  /**
   * Convert screen postition to model
   */
  private Point screen2model(Point p) {
    return new Point(
      p.x - content.getXOffset(),
      p.y - content.getYOffset()
    );
  }
  
  /** 
   * Update nodes arcs
   */
  private void updateArcs(Node node) {
    // update it's arcs
    ArcIterator it = new ArcIterator(node);
    while (it.next()) ArcHelper.update(it.arc);
    // show it
    repaint();
  }
  
  /**
   * Mouse Analyzer
   */
  private abstract class DnD extends MouseAdapter implements MouseMotionListener {
    /** start */
    protected void start(Point p) {
      // already someone there?
      if (dndCurrent!=null) {
        dndCurrent.stop();
        content.removeMouseListener(dndCurrent);
        content.removeMouseMotionListener(dndCurrent);
      }
      // switch
      dndCurrent = this;
      // start to listen
      content.addMouseListener(dndCurrent);
      content.addMouseMotionListener(dndCurrent);
      // done
    }
    /** stop */
    protected void stop() { }
    /** callback */
    public void mousePressed(MouseEvent e) {}
    /** callback */
    public void mouseReleased(MouseEvent e) {}
    /** callback */
    public void mouseDragged(MouseEvent e) {}
    /** callback */
    public void mouseMoved(MouseEvent e) {}
  } // EOC
  
  /**
   * Mouse Analyzer - Waiting
   */
  private class DnDIdle extends DnD {
    /** callback */
    public void mousePressed(MouseEvent e) {
      // nothing to do?
      if (graph==null) return;
      // something there?
      Node node = getElement(e.getPoint());
      if (node==null) {
        selection = null;
        repaint();
        return;
      }
      // new?
      if (node!=selection) {
        selection = node;
        repaint();
      }
      // start dragging?
      if ((e.getModifiers()&e.BUTTON1_MASK)!=0) {
        dndMoveNode.start(e.getPoint());
      }
      // done
    }
    /** callback */
    public void mouseReleased(MouseEvent e) {
      if ((e.getModifiers()&e.BUTTON3_MASK)!=0) {
        // popup
        JPopupMenu menu = selection!=null ? getNodePopupMenu(e.getPoint()) : getCanvasPopupMenu(e.getPoint());
        menu.show(content,e.getX(),e.getY());
        return;
      }
      if ((e.getModifiers()&e.BUTTON1_MASK)!=0) {
        if (quickNode) createNode(e.getPoint(),""+(graph.getNodes().size()+1));
        return;
      }
    }
  } //DnDIdle
  
  /**
   * Mouse Analyzer - Drag a Node
   */
  private class DnDMoveNode extends DnD {
    private Node node;
    private Point2D.Double offset = new Point2D.Double();
    /** start */
    protected void start(Point at) {
      super.start(at);
      node = getElement(at);
      offset.setLocation(
        at.getX() - node.getPosition().getX(),
        at.getY() - node.getPosition().getY()
      );
    }
    /** callback */
    public void mouseReleased(MouseEvent e) {
      dndNoOp.start(e.getPoint());
    }
    /** callback */
    public void mouseDragged(MouseEvent e) {
      // move the node
      node.getPosition().setLocation(
        e.getX() - offset.getX(),
        e.getY() - offset.getY()
      );
      // update after change of a Node
      updateArcs(node);
    }
  } //DnDMoveNode
  
  /**
   * Mouse Analyzer - Create an Arc
   */
  private class DnDCreateArc extends DnD{
    private Arc arc;
    private Node dummy;
    private Path path = new Path();
    /** start */
    protected void start(Point at) {
      super.start(at);
      dummy = graph.addNode(screen2model(at), null, null);      
      arc = graph.addArc(selection, dummy, path);
      repaint();
    }
    /** stop */
    protected void stop() {
      if (arc!=null) {
        graph.removeArc(arc);
        graph.removeNode(dummy);
        arc=null;
      }
    }
    /** callback */
    public void mouseReleased(MouseEvent e) {
      stop();
      Node to = getElement(e.getPoint());
      if (to!=null) {
        graph.addArc(selection, to, null);
        updateArcs(to);
      }
      repaint();
      dndNoOp.start(e.getPoint());
    }
    /** callback */
    public void mouseMoved(MouseEvent e) {
      dummy.getPosition().setLocation(screen2model(e.getPoint()));
      updateArcs(dummy);
    }
  } //DnDCreateArc

  /**
   * Mouse Analyzer - Resize a Node
   */
  private class DnDResizeNode extends DnD {
    /** our state */
    private Dimension2D.Double dim = new Dimension2D.Double();
    private Shape shape = null;
    private Point2D.Double factor = new Point2D.Double();
    /** start */
    protected void start(Point pos) {
      super.start(pos);
      // remember
      shape = selection.getShape();
      dim.setSize(selection.getShape().getBounds2D());
    }
    /** callback */
    public void mouseReleased(MouseEvent e) {
      dndNoOp.start(e.getPoint());
    }
    /** callback */
    public void mouseMoved(MouseEvent e) {
      // change shape
      Point2D oldPos = selection.getPosition();
      Point newPos = screen2model(e.getPoint());
      factor.setLocation(
        Math.max(0.1,Math.abs(newPos.x-oldPos.getX())/dim.w*2),
        Math.max(0.1,Math.abs(newPos.y-oldPos.getY())/dim.h*2)
      );
      GeneralPath gp = new GeneralPath(shape);
      gp.transform(AffineTransform.getScaleInstance(factor.x,factor.y));
      graph.setShape(selection,gp);
      updateArcs(selection);
      // show it
      repaint();
    }
  } //DnDResizeNode

  /**
   * How to handle - Delete a Node
   */
  private class ActionDeleteNode extends UnifiedAction {
    protected ActionDeleteNode() { super("Delete"); }
    protected void execute() {
      int i = SwingHelper.showDialog(GraphWidget.this, "Delete Node", "Are you sure?", SwingHelper.DLG_YES_NO);
      if (SwingHelper.OPTION_YES!=i) return;
      graph.removeNode(selection);
      selection=null;
      repaint();
    }
  } //ActionDeleteNode

  /**
   * How to handle - Sets a Node's Shape
   */
  private class ActionSetNodeShape extends UnifiedAction {
    Shape shape;
    protected ActionSetNodeShape(Shape set) {
      shape = set;
    }
    protected void execute() {
      graph.setShape(selection,shape);
      updateArcs(selection);
    }
  }

  /**
   * How to handle - Creats an arc
   */
  private class ActionCreateArc extends UnifiedAction {
    private Point start;
    protected ActionCreateArc(Point where) { super("Arc to"); start=where; }
    protected void execute() { dndCreateArc.start(start);  }
  }
  
  /**
   * How to handle - Change node content
   */
  private class ActionSetNodeContent extends UnifiedAction {
    protected ActionSetNodeContent() { super("Set content"); }
    protected void execute() {
      String txt = SwingHelper.showDialog(GraphWidget.this, "Set content", "Please enter text here:");
      if (txt==null) return;
      graph.setContent(selection,txt);
      repaint();
    }
  }

  /**
   * How to handle - Change node size
   */
  private class ActionResizeNode extends UnifiedAction {
    protected ActionResizeNode() { super("Resize"); }
    protected void execute() { dndResizeNode.start(null); }
  }
  
  /**
   * How to handle - Create a node
   */
  private class ActionCreateNode extends UnifiedAction {
    private Point pos;
    protected ActionCreateNode(Point set) { super("Create node"); pos = set; }
    protected void execute() {
      String txt = SwingHelper.showDialog(GraphWidget.this, "Set content", "Please enter text here:");
      if (txt==null) return;
      createNode(pos, txt);
    }
  }

  /**
   * How to handle - Toggle quick node
   */
  private class ActionToggleQuickNode extends UnifiedAction {
    protected ActionToggleQuickNode() { super("QuickNode"); }
    protected void execute() { quickNode=!quickNode; }
    public boolean isSelected() { return quickNode; }
  }
  
  /**
   * How to handle - NodePopupProvider's Action
   */
  private class ActionNodeLayoutMethod extends UnifiedAction {
    private Method method;
    protected ActionNodeLayoutMethod(Method method) { 
      super.setName(method.getName()+"()"); 
      this.method = method;
    }
    protected void execute() { 
      try {
        method.invoke(currentLayout, new Object[]{ selection });
      } catch (Exception e) {}
      repaint();
    }
  }
  
  /**
   * The content we use for drawing
   */
  private class Content extends JComponent {

    /**
     * Constructor
     */
    private Content() {
    } 
    
    /**
     * Calculate x offset for centered graph
     */
    private int getXOffset() {
      if (graph==null) return 0;
      return -graphBounds.x+(getWidth()-graphBounds.width)/2;
    }
    
    /**
     * Calculate y offset for centered graph
     */
    private int getYOffset() {
      if (graph==null) return 0;
      return -graphBounds.y+(getHeight()-graphBounds.height)/2;
    }
    
    /**
     * @see java.awt.Component#getPreferredSize()
     */
    public Dimension getPreferredSize() {
      if (graph==null) return new Dimension();
      return graphBounds.getSize();
    }
  
    /**
     * @see javax.swing.JComponent#paintComponent(Graphics)
     */
    protected void paintComponent(Graphics g) {
      
      // clear background
      g.setColor(Color.white);
      g.fillRect(0,0,getWidth(),getHeight());

      // graph there?
      if (graph==null) return;
      
      // cast to 2d
      Graphics2D graphics = (Graphics2D)g;
      
      // switch on antialiasing?
      graphics.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        isAntialiasing ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF
      );
      
      // synchronize on graph and go?
      synchronized (graph) {
        // create our working graphics
        // paint at 0,0
        graphics.translate(getXOffset(),getYOffset());
        // LayoutRenderer?
        if (layoutRenderer!=null) 
          layoutRenderer.render(graph,currentLayout,graphics);
        // let the renderer do its work
        graphRenderer.render(graph, currentLayout, graphics);
      }

      // done
    }
    
  } //Content
  
} //GraphWidget
