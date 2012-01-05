
package rtree;

import java.awt.event.MouseEvent;
import processing.core.PApplet;

import java.lang.reflect.Field;
import java.util.*;
import processing.core.PFont;

/**
 * R-Tree testing program. Make sure to not run with a security manager; the stuff that this renderer draws
 * is private to the R-Tree class and thus this program makes heavy use of java.lang.reflect.Field's!
 * @author Colonel32
 */
public class ProcessingTreeTester extends PApplet
{
	private interface Query
	{
		public void draw();
		public Set<BoundedObject> getResults();
	}

	private class BoxQuery implements Query
	{
		protected AABB box;
		protected Set<BoundedObject> results;
		public BoxQuery(AABB query)
		{
			box = query;
			results = new HashSet<BoundedObject>();
			testtree.query(results, box);
		}
		public Set<BoundedObject> getResults() { return results; }
		public void draw()
		{
			stroke(240,1,1);
			noFill();
			drawBox(box);
		}
	}

	private class PointQuery implements Query
	{
		private int x,y,z;
		private Set<BoundedObject> results;
		public PointQuery(int px, int py, int pz)
		{
			x = px;
			y = py;
			z = pz;
			results = new HashSet<BoundedObject>();
			testtree.query(results,x,y,z);
		}
		public Set<BoundedObject> getResults() { return results; }
		public void draw()
		{
			pushMatrix();
			noStroke();
			fill(240,1,1);
			translate(x,y,z);
			sphere(5);
			popMatrix();
		}
	}

	public static RTree testtree;
	private static Query query;

	// Arguments
	public static int treeMin, treeMax, dataCount;

	// Unchangeable settings
	public static AABB dataRealm;
	public static AABB dataSize;
	public static AABB querySize;

	public static Field nodeBoxField;
	public static Field nodeChildrenField;
	public static Field nodeDataField;
	public static Field treeRootField;

	public static ArcBall ball;
	public static PFont font;

	public void setup()
	{
		size(600,600,PApplet.P3D);
		smooth();
		font = createFont("Arial",12);
		textFont(font);
		colorMode(PApplet.HSB, 360, 1, 1);

		ball = new ArcBall(width/2,height/2,0,50,this);

		dataRealm = new AABB();
		dataRealm.setMinCorner(-200,-200,-200);
		dataRealm.setMaxCorner(200,200,200);

		dataSize = new AABB();
		dataSize.setMinCorner(5,5,5);
		dataSize.setMaxCorner(20, 20, 20);

		querySize = new AABB();
		querySize.setMinCorner(50, 50, 50);
		querySize.setMaxCorner(150, 150, 150);

		System.out.println(
				  "-------------------------------\n"
				+ "--- 3D R-Tree Test Program ---\n"
				+ "-------------------------------\n"
				+ "Instructions:\n"
				+ "LMB + Drag Mouse: Rotate view\n"
				+ "S: Save screenshot in current directory\n"
				+ "G: Generate new tree\n"
				+ "B: Generate new box query\n"
				+ "P: Generate new point query\n"
				+ "-------------------------------");
		generateTree();
	}

	private static void parseArguments(String[] args)
	{
		treeMin = treeMax = dataCount = 0;
		for(int i=0; i<args.length; i++)
		{
			if(args[i].equals("--count"))
			{
				i++;
				if(i >= args.length) error("Expected argument after --count");
				try
				{
					dataCount = Integer.parseInt(args[i]);
				}
				catch(NumberFormatException e)
				{
					error("Invalid number format: "+args[i]);
				}

				if(dataCount <= 0)
				{
					error("Count argument must be greater than zero.");
				}
			}
			else if(args[i].equals("--min"))
			{
				i++;
				if(i >= args.length) error("Expected argument after --min");
				try
				{
					treeMin = Integer.parseInt(args[i]);
				}
				catch(NumberFormatException e)
				{
					error("Invalid number format: "+args[i]);
				}

				if(treeMin < 2)
				{
					error("Tree minimum argument must be greater than or equal to 2.");
				}
			}
			else if(args[i].equals("--max"))
			{
				i++;
				if(i >= args.length) error("Expected argument after --max");
				try
				{
					treeMax = Integer.parseInt(args[i]);
				}
				catch(NumberFormatException e)
				{
					error("Invalid number format: "+args[i]);
				}

				if(treeMax < 4)
				{
					error("Tree maximum argument must be greater than or equal to 4.");
				}
			}
			else if(args[i].equals("--help"))
			{
				System.out.println(
						  "--------------------------------\n"
						+ "--- R-Tree Tester ---\n"
						+ "Arguments:\n"
						+ "--count <n>: Generate n amount of test items (default 50)\n"
						+ "--min <n>: Sets n to the minimum node size (default 5)\n"
						+ "--max <n>: Sets n to the maximum node size (default 10)\n"
						+ "--------------------------------");
				System.exit(0);
				return;
			}
		}

		if(treeMin == 0) treeMin = 5;
		if(treeMax == 0) treeMax = 10;
		if(dataCount == 0) dataCount = 50;
	}

	private static void error(String msg)
	{
		System.err.println(msg);
		System.exit(1);
	}

	public void generateTree()
	{
		System.out.println("Generating new tree...");
		testtree = new RTree(treeMin, treeMax);
		try
		{
			Class nodeClass = Class.forName("rtree.RTree$Node");
			nodeBoxField = nodeClass.getDeclaredField("box");
			nodeBoxField.setAccessible(true);
			nodeChildrenField = nodeClass.getDeclaredField("children");
			nodeChildrenField.setAccessible(true);
			nodeDataField = nodeClass.getDeclaredField("data");
			nodeDataField.setAccessible(true);

			treeRootField = RTree.class.getDeclaredField("root");
			treeRootField.setAccessible(true);
		}
		catch(ClassNotFoundException e)
		{
			System.err.println("Internal error: ClassNotFoundException: "+e.getMessage());
			exit();
		}
		catch(NoSuchFieldException e)
		{
			System.err.println("Internal error: NoSuchFieldException: "+e.getMessage());
			exit();
		}

		Random rand = new Random();
		for(int i=0; i<dataCount; i++)
		{
			AABB box = randomBox(dataRealm, dataSize, rand);
			System.out.println("Generating box "+(i+1)+": "+box.toString());
			testtree.insert(box);
		}
		System.out.println("Tree size: "+testtree.count());
	}

	/**
	 * Generates a random AABB inside of posrange, with size defined by sizerange.
	 * @param posrange Area that the new AABB should be generated in.
	 * @param sizerange Minimum (inclusive) and maximum (exclusive) size of the generated AABB.
	 * @param rand Random number generator to use
	 * @throws IllegalArgumentException If a box of the maximum size specified by sizerange cannot fit into posrange
	 */
	public static AABB randomBox(AABB posrange, AABB sizerange, Random rand)
	{
		if(sizerange.maxx-1 > posrange.maxx - posrange.minx) throw new IllegalArgumentException("Generated AABB might not be within posrange.");
		if(sizerange.maxy-1 > posrange.maxy - posrange.miny) throw new IllegalArgumentException("Generated AABB might not be within posrange.");
		if(sizerange.maxz-1 > posrange.maxz - posrange.minz) throw new IllegalArgumentException("Generated AABB might not be within posrange.");

		AABB box = new AABB();
		int dx = rand.nextInt(sizerange.maxx - sizerange.minx)+sizerange.minx;
		int dy = rand.nextInt(sizerange.maxy - sizerange.miny)+sizerange.miny;
		int dz = rand.nextInt(sizerange.maxz - sizerange.minz)+sizerange.minz;

		int x = rand.nextInt(posrange.maxx - posrange.minx - dx) + posrange.minx;
		int y = rand.nextInt(posrange.maxy - posrange.miny - dx) + posrange.miny;
		int z = rand.nextInt(posrange.maxz - posrange.minz - dx) + posrange.minz;

		box.setMinCorner(x, y, z);
		box.setMaxCorner(x+dx, y+dy, z+dz);

		return box;
	}

	public void draw()
	{
		background(0);
		lights();

		pushMatrix();
		translate(width/2,height/2,0);
		pointLight(0,0,1,0,0,0);
		specular(100);
		noStroke();

		try
		{
			draw(treeRootField.get(testtree),0);

			if(query != null)
			{
				query.draw();
				fill(240,1,1);
				stroke(240,0.5f,0.5f);
				for(BoundedObject o : query.getResults())
					drawBox(o.getBounds());
			}
		}
		catch(IllegalAccessException e)
		{
			System.err.println("Couldn't access private field; is a security manager running?");
			exit();
		}
		popMatrix();
	}

	@SuppressWarnings("unchecked")
	private void draw(Object node, int depth) throws IllegalAccessException
	{
		if(node == null) return;
		noFill();
		stroke((depth*50)%360, 1, 1);

		AABB box = (AABB)nodeBoxField.get(node);
		ArrayList children = (ArrayList)nodeChildrenField.get(node);
		ArrayList<BoundedObject> data = (ArrayList<BoundedObject>)nodeDataField.get(node);

		drawBox(box);
		if(children == null)
		{
			fill(((depth+1)*50)%360,1,1);
			stroke(((depth+1)*50)%360,0.5f,0.5f);
			for(int i=0; i<data.size(); i++)
			{
				AABB abox = data.get(i).getBounds();
				if(query == null || !query.getResults().contains(abox))
					drawBox(abox);
			}
		}
		else
			for(int i=0; i<children.size(); i++)
				draw(children.get(i), depth+1);
	}

	private void drawBox(AABB box)
	{
		pushMatrix();
		translate((box.minx+box.maxx)/2.0f, (box.miny+box.maxy)/2.0f, (box.minz+box.maxz)/2.0f);
		box((box.maxx-box.minx), (box.maxy-box.miny), (box.maxz-box.minz));
		//translate(box.minx, box.miny, box.minz);
		//box(box.maxx - box.minx, box.maxy-box.miny, box.maxz-box.minz);
		popMatrix();
	}

	public static void main(String[] argz)
	{
		parseArguments(argz);
		PApplet.main(new String[] {"rtree.ProcessingTreeTester"});
	}

	/**
	 * Taken from the processing wiki. Rotates the screen based on LMB drags.
	 */
	public static class ArcBall
	{

		  PApplet parent;

		  float center_x, center_y, center_z, radius;
		  Vec3 v_down, v_drag;
		  Quat q_now, q_down, q_drag;
		  Vec3[] axisSet;
		  int axis;

		  /** defaults to radius of min(width/2,height/2) and center_z of -radius */
		  public ArcBall(PApplet parent) {
			this(parent.g.width/2.0f,parent.g.height/2.0f,-PApplet.min(parent.g.width/2.0f,parent.g.height/2.0f),PApplet.min(parent.g.width/2.0f,parent.g.height/2.0f), parent);
		  }

		  public ArcBall(float center_x, float center_y, float center_z, float radius, PApplet parent) {

			this.parent = parent;

			parent.registerMouseEvent(this);
			parent.registerPre(this);

			this.center_x = center_x;
			this.center_y = center_y;
			this.center_z = center_z;
			this.radius = radius;

			v_down = new Vec3();
			v_drag = new Vec3();

			q_now = new Quat();
			q_down = new Quat();
			q_drag = new Quat();

			axisSet = new Vec3[] {
			  new Vec3(1.0f, 0.0f, 0.0f), new Vec3(0.0f, 1.0f, 0.0f), new Vec3(0.0f, 0.0f, 1.0f) };
			axis = -1;  // no constraints...
		  }

		  public void mouseEvent(MouseEvent event) {
			int id = event.getID();
			if (id == MouseEvent.MOUSE_DRAGGED) {
			  mouseDragged();
			}
			else if (id == MouseEvent.MOUSE_PRESSED) {
			  mousePressed();
			}
		  }

		  public void mousePressed() {
			v_down = mouse_to_sphere(parent.mouseX, parent.mouseY);
			q_down.set(q_now);
			q_drag.reset();
		  }

		  public void mouseDragged() {
			v_drag = mouse_to_sphere(parent.mouseX, parent.mouseY);
			q_drag.set(Vec3.dot(v_down, v_drag), Vec3.cross(v_down, v_drag));
		  }

		  public void pre() {
			parent.translate(center_x, center_y, center_z);
			q_now = Quat.mul(q_drag, q_down);
			applyQuat2Matrix(q_now);
			parent.translate(-center_x, -center_y, -center_z);
		  }

		  Vec3 mouse_to_sphere(float x, float y) {
			Vec3 v = new Vec3();
			v.x = (x - center_x) / radius;
			v.y = (y - center_y) / radius;

			float mag = v.x * v.x + v.y * v.y;
			if (mag > 1.0f) {
			  v.normalize();
			}
			else {
			  v.z = PApplet.sqrt(1.0f - mag);
			}

			return (axis == -1) ? v : constrain_vector(v, axisSet[axis]);
		  }

		  Vec3 constrain_vector(Vec3 vector, Vec3 axis) {
			Vec3 res = new Vec3();
			res.sub(vector, Vec3.mul(axis, Vec3.dot(axis, vector)));
			res.normalize();
			return res;
		  }

		  void applyQuat2Matrix(Quat q) {
			// instead of transforming q into a matrix and applying it...

			float[] aa = q.getValue();
			parent.rotate(aa[0], aa[1], aa[2], aa[3]);
		  }

		  static class Vec3 {
			float x, y, z;

			Vec3() {}

			Vec3(float x, float y, float z) {
			  this.x = x;
			  this.y = y;
			  this.z = z;
			}

			void normalize() {
			  float length = length();
			  x /= length;
			  y /= length;
			  z /= length;
			}

			float length() {
			  return PApplet.mag(x,y,z);
			}

			static Vec3 cross(Vec3 v1, Vec3 v2) {
			  Vec3 res = new Vec3();
			  res.x = v1.y * v2.z - v1.z * v2.y;
			  res.y = v1.z * v2.x - v1.x * v2.z;
			  res.z = v1.x * v2.y - v1.y * v2.x;
			  return res;
			}

			static float dot(Vec3 v1, Vec3 v2) {
			  return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
			}

			static Vec3 mul(Vec3 v, float d) {
			  Vec3 res = new Vec3();
			  res.x = v.x * d;
			  res.y = v.y * d;
			  res.z = v.z * d;
			  return res;
			}

			void sub(Vec3 v1, Vec3 v2) {
			  x = v1.x - v2.x;
			  y = v1.y - v2.y;
			  z = v1.z - v2.z;
			}

		  } // Vec3

		  static class Quat {

			float w, x, y, z;

			Quat() {
			  reset();
			}

			Quat(float w, float x, float y, float z) {
			  this.w = w;
			  this.x = x;
			  this.y = y;
			  this.z = z;
			}

			void reset() {
			  w = 1.0f;
			  x = 0.0f;
			  y = 0.0f;
			  z = 0.0f;
			}

			void set(float w, Vec3 v) {
			  this.w = w;
			  x = v.x;
			  y = v.y;
			  z = v.z;
			}

			void set(Quat q) {
			  w = q.w;
			  x = q.x;
			  y = q.y;
			  z = q.z;
			}

			static Quat mul(Quat q1, Quat q2) {
			  Quat res = new Quat();
			  res.w = q1.w * q2.w - q1.x * q2.x - q1.y * q2.y - q1.z * q2.z;
			  res.x = q1.w * q2.x + q1.x * q2.w + q1.y * q2.z - q1.z * q2.y;
			  res.y = q1.w * q2.y + q1.y * q2.w + q1.z * q2.x - q1.x * q2.z;
			  res.z = q1.w * q2.z + q1.z * q2.w + q1.x * q2.y - q1.y * q2.x;
			  return res;
			}

			float[] getValue() {
			  // transforming this quat into an angle and an axis vector...

			  float[] res = new float[4];

			  float sa = (float) Math.sqrt(1.0f - w * w);
			  if (sa < PApplet.EPSILON) {
				sa = 1.0f;
			  }

			  res[0] = (float) Math.acos(w) * 2.0f;
			  res[1] = x / sa;
			  res[2] = y / sa;
			  res[3] = z / sa;

			  return res;
			}

		  } // Quat

		}
	public void mousePressed()
	{
		if(mouseButton == PApplet.RIGHT)
		{
			saveFrame();
			System.out.println("Saved screenshot.");
		}
	}

	public void keyPressed()
	{
		if(key == 'b')
		{
			query = new BoxQuery(randomBox(dataRealm, querySize, new Random()));
		}
		else if(key == 'p')
		{
			Random rand = new Random();
			int x = rand.nextInt(dataRealm.maxx - dataRealm.minx) + dataRealm.minx;
			int y = rand.nextInt(dataRealm.maxy - dataRealm.miny) + dataRealm.miny;
			int z = rand.nextInt(dataRealm.maxz - dataRealm.minz) + dataRealm.minz;
			query = new PointQuery(x,y,z);
		}
		else if(key == 'g')
			generateTree();
		else if(key == 's')
		{
			saveFrame();
			System.out.println("Saved screenshot.");
		}
	}
}
