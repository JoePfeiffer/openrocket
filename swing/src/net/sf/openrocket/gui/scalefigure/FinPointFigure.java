package net.sf.openrocket.gui.scalefigure;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.*;
import net.sf.openrocket.rocketcomponent.FreeformFinSet;
import net.sf.openrocket.rocketcomponent.RocketComponent;
import net.sf.openrocket.rocketcomponent.SymmetricComponent;
import net.sf.openrocket.rocketcomponent.Transition;
import net.sf.openrocket.rocketcomponent.position.AxialMethod;
import net.sf.openrocket.unit.Tick;
import net.sf.openrocket.unit.Unit;
import net.sf.openrocket.unit.UnitGroup;
import net.sf.openrocket.util.BoundingBox;
import net.sf.openrocket.util.Coordinate;
import net.sf.openrocket.util.MathUtil;
import net.sf.openrocket.util.StateChangeListener;


@SuppressWarnings("serial")
public class FinPointFigure extends AbstractScaleFigure {
 
    private final static Logger log = LoggerFactory.getLogger(FinPointFigure.class);

    private static final Color GRID_LINE_COLOR = new Color( 137, 137, 137, 32);
    private static final float GRID_LINE_BASE_WIDTH = 0.001f;

    private static final int LINE_WIDTH_PIXELS = 1;
    
    // the size of the boxes around each fin point vertex
    private static final float BOX_WIDTH_PIXELS = 12; 
    private static final float SELECTED_BOX_WIDTH_PIXELS = 16;
        
    private static final double MINOR_TICKS = 0.05;
    private static final double MAJOR_TICKS = 0.1;
            
    private final FreeformFinSet finset;
	private int modID = -1;
	
	protected Rectangle2D finBounds_m = null;
	protected Rectangle2D mountBounds_m = null;
	
    protected final List<StateChangeListener> listeners = new LinkedList<StateChangeListener>();
	       
    private Rectangle2D.Double[] finPointHandles = null;
    private int selectedIndex = -1;
	
	public FinPointFigure(FreeformFinSet finset) {
		this.finset = finset;
		
		// useful for debugging -- shows a contrast against un-drawn space.
		setBackground(Color.WHITE);
		setOpaque(true);

		updateFigure();        
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
                
        if (modID != finset.getRocket().getAerodynamicModID()) {
            modID = finset.getRocket().getAerodynamicModID(); 
            updateTransform();
        }
                
		g2.transform(projection);
		
		// Set rendering hints appropriately
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
				RenderingHints.VALUE_STROKE_NORMALIZE);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		

        // Background grid
		paintBackgroundGrid( g2);

		paintRocketBody(g2);
		
		paintFinShape(g2);
		paintFinHandles(g2);	
	}
	
	public void paintBackgroundGrid( Graphics2D g2){
	    Rectangle visible = g2.getClipBounds();
	    int x0 = visible.x - 3;
	    int x1 = visible.x + visible.width + 4;
	    int y0 = visible.y - 3;
	    int y1 = visible.y + visible.height + 4;

	    final float grid_line_width = (float)(FinPointFigure.GRID_LINE_BASE_WIDTH/this.scale);
	    g2.setStroke(new BasicStroke( grid_line_width,
	            BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
	    g2.setColor(FinPointFigure.GRID_LINE_COLOR);

	    Unit unit;
	    if (this.getParent() != null && this.getParent().getParent() instanceof ScaleScrollPane) {
	        unit = ((ScaleScrollPane) this.getParent().getParent()).getCurrentUnit();
	    } else {
	        unit = UnitGroup.UNITS_LENGTH.getDefaultUnit();
	    }

	    // vertical
	    Tick[] verticalTicks = unit.getTicks(x0, x1, MINOR_TICKS, MAJOR_TICKS);
	    Line2D.Double line = new Line2D.Double();
	    for (Tick t : verticalTicks) {
	        if (t.major) {
	            line.setLine( t.value, y0, t.value, y1);
	            g2.draw(line);
	        }
	    }

	    // horizontal
	    Tick[] horizontalTicks = unit.getTicks(y0, y1, MINOR_TICKS, MAJOR_TICKS);
	    for (Tick t : horizontalTicks) {
	        if (t.major) {
	            line.setLine( x0, t.value, x1, t.value);
	            g2.draw(line);
	        }
	    }
	}

    private void paintRocketBody( Graphics2D g2){
        RocketComponent comp = finset.getParent();
        if( comp instanceof Transition ){
            paintBodyTransition(g2);
        }else{
            paintBodyTube(g2);                      
        }
    }

    // NOTE:  This function drawns relative to the reference point of the BODY component
    // In other words: 0,0 == the front, foreRadius of the body component
    private void paintBodyTransition( Graphics2D g2){       
    
        // setup lines 
        final float bodyLineWidth = (float) ( LINE_WIDTH_PIXELS / scale ); 
        g2.setStroke(new BasicStroke( bodyLineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        g2.setColor(Color.BLACK);

        Transition body = (Transition) finset.getParent();
        final float xResolution_m = 0.01f; // distance between draw points, in meters

        final double xFinStart = finset.asPositionValue(AxialMethod.TOP); //<<  in body frame

        // vv in fin-frame == draw-frame vv
        final double xOffset = -xFinStart;
        final double yOffset = -body.getRadius(xFinStart);

        Path2D.Double bodyShape = new Path2D.Double();
        // draw front-cap: 
        bodyShape.moveTo( xOffset, yOffset);
        bodyShape.lineTo( xOffset, yOffset + body.getForeRadius());

        final float length_m = (float)( body.getLength());
        Point2D.Double cur = new Point2D.Double ();
        for( double xBody = xResolution_m ; xBody < length_m;  xBody += xResolution_m ){
            // xBody is distance from front of parent body
            cur.x = xOffset + xBody; // offset from origin (front of fin)
            cur.y = yOffset + body.getRadius( xBody); // offset from origin ( fin-front-point ) 

            bodyShape.lineTo( cur.x, cur.y);
        }

        // draw end-cap
        bodyShape.lineTo( xOffset + length_m, yOffset + body.getAftRadius());
        bodyShape.lineTo( xOffset + length_m, yOffset);

        g2.draw(bodyShape);
    }

    private void paintBodyTube( Graphics2D g2){
        // in-figure left extent
        final double xFore = mountBounds_m.getMinX();
        // in-figure right extent
        final double xAft = mountBounds_m.getMaxX();
        // in-figure right extent
        final double yCenter = mountBounds_m.getMinY();
        
        Path2D.Double shape = new Path2D.Double();
        shape.moveTo( xFore, yCenter );
        shape.lineTo( xFore, 0);         // body tube fore edge
        shape.lineTo( xAft, 0);          // body tube side
        shape.lineTo( xAft, yCenter);    // body tube aft edge
        
        final float bodyLineWidth = (float) ( LINE_WIDTH_PIXELS / scale ); 
        g2.setStroke(new BasicStroke( bodyLineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        g2.setColor(Color.BLACK);
        g2.draw(shape);
    }

	private void paintFinShape(final Graphics2D g2){
	    // excludes fin tab points
	    final Coordinate[] drawPoints = finset.getFinPoints();
             
	    Path2D.Double shape = new Path2D.Double();
	    Coordinate startPoint= drawPoints[0];
	    shape.moveTo( startPoint.x, startPoint.y);
	    for (int i = 1; i < drawPoints.length; i++) {
	        shape.lineTo( drawPoints[i].x, drawPoints[i].y);
	    }
        
	    final float finEdgeWidth_m = (float) (LINE_WIDTH_PIXELS / scale  );
	    g2.setStroke(new BasicStroke( finEdgeWidth_m, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
	    g2.setColor(Color.BLUE);
	    g2.draw(shape);
	}
	
	private void paintFinHandles(final Graphics2D g2) {
	    // excludes fin tab points
        final Coordinate[] drawPoints = finset.getFinPoints();
       
	    // Fin point boxes
        final float boxWidth = (float) (BOX_WIDTH_PIXELS / scale );
        final float boxHalfWidth = boxWidth/2;
        final float selBoxWidth = (float) (SELECTED_BOX_WIDTH_PIXELS / scale );
        final float selBoxHalfWidth = boxWidth/2;

        final float boxEdgeWidth_m = (float) ( LINE_WIDTH_PIXELS / scale );
        g2.setStroke(new BasicStroke( boxEdgeWidth_m, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        g2.setColor(new Color(150, 0, 0));
        
        finPointHandles = new Rectangle2D.Double[ drawPoints.length];
        for (int currentIndex = 0; currentIndex < drawPoints.length; currentIndex++) {
            Coordinate c = drawPoints[currentIndex];
            
            if( currentIndex == selectedIndex ) {
                finPointHandles[currentIndex] = new Rectangle2D.Double(c.x - selBoxHalfWidth, c.y - selBoxHalfWidth, selBoxWidth, selBoxWidth);
            } else {
                // normal boxes
                finPointHandles[currentIndex] = new Rectangle2D.Double(c.x - boxHalfWidth, c.y - boxHalfWidth, boxWidth, boxWidth);
            }
            
            g2.draw(finPointHandles[currentIndex]);
        }
    }

	private Point2D.Double getPoint( final int x, final int y){
	    if (finPointHandles == null)
            return null;
         
         // Calculate point in shapes' coordinates
         Point2D.Double p = new Point2D.Double(x, y);
         try {
                 projection.inverseTransform(p, p);
                 return p;
         } catch (NoninvertibleTransformException e) {
                 return null;
         }
	}
	
    public int getIndexByPoint(final int x, final int y) {
        final Point2D.Double p = getPoint(x,y); 
        if (p == null)
            return -1;
        
         for (int i = 0; i < finPointHandles.length; i++) {
             if (finPointHandles[i].contains(p)) {
                 return i;
             }
         }
         
         return -1;
    }

	public int getSegmentByPoint(final int x, final int y) {
	    final Point2D.Double p = getPoint(x,y);          
        if (p == null)
             return -1;

		final double threshold = BOX_WIDTH_PIXELS / scale;
		
		Coordinate[] points = finset.getFinPoints();
		for (int i = 1; i < points.length; i++) {
			double x1 = points[i - 1].x;
			double y1 = points[i - 1].y;
			double x2 = points[i].x;
			double y2 = points[i].y;
			
			final double segmentLength = MathUtil.hypot(x2 - x1, y2 - y1);

			// Distance to an infinite line, defined by two points:
			// (For a more in-depth explanation, see wikipedia: https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line )
			double x0 = p.x;
            double y0 = p.y;
            final double distanceToLine = Math.abs((y2 - y1)*x0 - (x2-x1)*y0 + x2*y1 - y2*x1)/segmentLength;
						                
            final double distanceToStart = MathUtil.hypot(x1-x0, y1-y0); 
            final double distanceToEnd = MathUtil.hypot(x2-x0, y2-y0); 
            final boolean withinSegment = (distanceToStart < segmentLength && distanceToEnd < segmentLength);
            
			if ( distanceToLine < threshold && withinSegment){
			    return i;
			}

		}
		
		return -1;
	}
	
	public Point2D.Double convertPoint(final double x, final double y) {
		Point2D.Double p = new Point2D.Double(x, y);
		try {
			projection.inverseTransform(p, p);
		} catch (NoninvertibleTransformException e) {
			assert (false) : "Should not occur";
			return new Point2D.Double(0, 0);
		}
		
		p.setLocation(p.x, p.y);
		return p;
	}

	public Dimension getSubjectOrigin() {
		if (modID != finset.getRocket().getAerodynamicModID()) {
			modID = finset.getRocket().getAerodynamicModID();
			updateTransform();
		}
        return new Dimension(originLocation_px.width, originLocation_px.height);
    }
    
	@Override
    protected void updateSubjectDimensions(){
        // update subject (i.e. Fin) bounds
	    finBounds_m = new BoundingBox().update(finset.getFinPoints()).toRectangle();
	    // NOTE: the fin's forward root is pinned at 0,0  
	    finBounds_m.setRect(0, 0, finBounds_m.getWidth(), finBounds_m.getHeight());
	    
	    // update to bound the parent body:
	    SymmetricComponent parent = (SymmetricComponent)this.finset.getParent(); 
        final double xParent = - finset.asPositionValue(AxialMethod.TOP); //<<  in body frame
	    final double yParent = -parent.getRadius(xParent); // from parent centerline to fin front.
	    final double rParent = Math.max(parent.getForeRadius(), parent.getAftRadius());
	    mountBounds_m = new Rectangle2D.Double( xParent, yParent, parent.getLength(), rParent);
                
	    final double subjectWidth = Math.max( finBounds_m.getWidth(), parent.getLength());
	    final double subjectHeight = Math.max( 2*rParent, rParent + finBounds_m.getHeight());
	    subjectBounds_m = new Rectangle2D.Double( xParent, yParent, subjectWidth, subjectHeight);
	}

	@Override
    protected void updateCanvasOrigin() {
	    final SymmetricComponent parent = (SymmetricComponent)this.finset.getParent();
	    final double rMaxParent = Math.max(parent.getForeRadius(), parent.getAftRadius());
	    
        // the negative sign is to compensate for the mount's negative location.
        originLocation_px.width = borderThickness_px.width - (int)(mountBounds_m.getX()*scale);
        originLocation_px.height = borderThickness_px.height + (int)(Math.max( rMaxParent, finBounds_m.getHeight())*scale);
        
        System.err.println(String.format("________ Origin Location (px): w=%d, h=%d: ", originLocation_px.width, originLocation_px.height));
    }

    public void resetSelectedIndex() {
        this.selectedIndex = -1;
    }

    public void setSelectedIndex(final int newIndex) {
        this.selectedIndex = newIndex;
    }

}
