
package rtree;

/**
 * 3D Axis Aligned Bounding Box
 * @author Colonel32
 */
public class AABB implements BoundedObject
{
	int minx, miny, minz;
	int maxx, maxy, maxz;

	public AABB()
	{
		minx = miny = minz = 0;
		maxx = maxy = maxz = 0;
	}

	public void setMinCorner(int px, int py, int pz)
	{
		minx = px;
		miny = py;
		minz = pz;
	}
	public void setMaxCorner(int px, int py, int pz)
	{
		maxx = px;
		maxy = py;
		maxz = pz;
	}

	public boolean contains(int px, int py, int pz)
	{
		return px >= minx && px <= maxx &&
				py >= miny && py <= maxy &&
				pz >= minz && pz <= maxz;
	}

	public boolean overlaps(AABB other)
	{
		if(minx > other.maxx) return false;
		if(maxx < other.minx) return false;
		if(miny > other.maxy) return false;
		if(maxy < other.miny) return false;
		if(minz > other.maxz) return false;
		if(maxz < other.minz) return false;
		return true;
	}
	
	/**
	 * Returns the amount of overlap between 2 AABBs. Result will be negative if they
	 * do not overlap.
	 */
	public int getOverlap(AABB other)
	{
		int overlapx =  (maxx - minx +other.maxx - other.minx) - Math.abs(minx+maxx-other.minx-other.minx);
		int overlapy =  (maxy - miny +other.maxy - other.miny) - Math.abs(minx+maxy-other.miny-other.miny);
		int overlapz =  (maxz - minz +other.maxz - other.minz) - Math.abs(minx+maxz-other.minz-other.minz);
		
		return Math.max(overlapx, Math.max(overlapy, overlapz));
		
	}

	/**
	 * Returns the amount that other will need to be expanded to fit this.
	 */
	public int expansionNeeded(AABB other)
	{
		int total = 0;

		if(other.minx < minx) total += minx - other.minx;
		if(other.maxx > maxx) total += other.maxx - maxx;

		if(other.miny < miny) total += miny - other.miny;
		if(other.maxy > maxy) total += other.maxy - maxy;

		if(other.minz < minz) total += minz - other.minz;
		if(other.maxz > maxz) total += other.maxz - maxz;

		return total;
	}

	/**
	 * Computes an AABB that contains both this and other and stores it in this.
	 * @return this
	 */
	public AABB merge(AABB other)
	{
		minx = Math.min(minx, other.minx);
		maxx = Math.max(maxx, other.maxx);

		miny = Math.min(miny, other.miny);
		maxy = Math.max(maxy, other.maxy);

		minz = Math.min(minz, other.minz);
		maxz = Math.max(maxz, other.maxz);

		return this;
	}

	public int getVolume()
	{
		return (maxx - minx) * (maxy - miny) * (maxz - minz);
	}

	public AABB clone()
	{
		AABB clone = new AABB();
		clone.minx = minx;
		clone.miny = miny;
		clone.minz = minz;

		clone.maxx = maxx;
		clone.maxy = maxy;
		clone.maxz = maxz;
		return clone;
	}

	public void cloneInto(AABB target)
	{
		target.minx = minx;
		target.miny = miny;
		target.minz = minz;

		target.maxx = maxx;
		target.maxy = maxy;
		target.maxz = maxz;
	}

	public boolean equals(AABB other)
	{
		return minx == other.minx && maxx == other.maxx &&
				miny == other.miny && maxy == other.maxy &&
				minz == other.minz && maxz == other.maxz;
	}

	public AABB getBounds() { return this; }
	public String toString()
	{
		return String.format("(%1$d,%2$d,%3$d):(%4$d,%5$d,%6$d)", minx, miny, minz, maxx, maxy, maxz);
	}
}
