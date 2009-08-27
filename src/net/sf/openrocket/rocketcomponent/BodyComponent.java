package net.sf.openrocket.rocketcomponent;


/**
 * Class to represent a body object.  The object can be described as a function of
 * the cylindrical coordinates x and angle theta as  r = f(x,theta).  The component 
 * need not be symmetrical in any way (e.g. square tube, slanted cone etc).
 * 
 * It defines the methods getRadius(x,theta) and getInnerRadius(x,theta), as well
 * as get/setLength().
 * 
 * @author Sampo Niskanen <sampo.niskanen@iki.fi>
 */

public abstract class BodyComponent extends ExternalComponent {

	/**
	 * Default constructor.  Sets the relative position to POSITION_RELATIVE_AFTER,
	 * i.e. body components come after one another.
	 */
	public BodyComponent() {
		super(RocketComponent.Position.AFTER);
	}
	
	
	
	/**
	 * Get the outer radius of the component at cylindrical coordinate (x,theta).
	 * 
	 * Note that the return value may be negative for a slanted object.
	 * 
	 * @param x  Distance in x direction
	 * @param theta  Angle about the x-axis
	 * @return  Distance to the outer edge of the object
	 */
	public abstract double getRadius(double x, double theta);

	
	/**
	 * Get the inner radius of the component at cylindrical coordinate (x,theta).
	 * 
	 * Note that the return value may be negative for a slanted object.
	 * 
	 * @param x  Distance in x direction
	 * @param theta  Angle about the x-axis
	 * @return  Distance to the inner edge of the object
	 */
	public abstract double getInnerRadius(double x, double theta);

	

	/**
	 * Sets the length of the body component.
	 */
	public void setLength(double length) {
		if (this.length == length)
			return;
		this.length = Math.max(length,0);
		fireComponentChangeEvent(ComponentChangeEvent.BOTH_CHANGE);
	}
	
	
	/**
	 * Check whether the given type can be added to this component.  BodyComponents allow any
	 * InternalComponents or ExternalComponents, excluding BodyComponents, to be added.
	 * 
	 * @param type  The RocketComponent class type to add.
	 * @return      Whether such a component can be added.
	 */
	@Override
	public boolean isCompatible(Class<? extends RocketComponent> type) {
		if (InternalComponent.class.isAssignableFrom(type))
			return true;
		if (ExternalComponent.class.isAssignableFrom(type) &&
			!BodyComponent.class.isAssignableFrom(type))
			return true;
		return false;
	}
}