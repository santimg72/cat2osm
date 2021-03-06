import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;


public class ShapeParcela extends Shape {
	
	private String shapeId = null; // Id del shape PARCELA+long
	private List<LineString> poligons; // [0] Outer, [1..N] inner
	private List<List<Long>> nodes; // [0] Outer, [1..N] inner
	private List<List<Long>> ways; // [0] Outer, [1..N] inner
	private Long relation; // Relacion de sus ways
	private String refCatastral; // Referencia catastral
	private List<ShapeAttribute> atributos;
	private int numSymbol;
	
	private HashMap<String,Double> usos; // Para definir cual de todos los usos y destinos asignar,
	// se ha llegado a la conclusion de asignar el que mas area tenga


	/** Constructor
	 * @param f Linea del archivo shp
	 */
	public ShapeParcela(SimpleFeature f, String tipo) {

		super(f, tipo);

		shapeId = "PARCELA" + super.newShapeId();
		
		this.poligons = new ArrayList<LineString>();

		// Parcela.shp trae la geometria en formato MultiPolygon
		if ( f.getDefaultGeometry().getClass().getName().equals("com.vividsolutions.jts.geom.MultiPolygon")){

			// Poligono, trae el primer punto de cada poligono repetido al final.
			Geometry geom = (Geometry) f.getDefaultGeometry();
			
			// Cogemos cada poligono del shapefile (por lo general sera uno solo
			// que puede tener algun subpoligono)
			for (int x = 0; x < geom.getNumGeometries(); x++) { 
				Polygon p = (Polygon) geom.getGeometryN(x); 

				// Obtener el outer
				LineString outer = p.getExteriorRing();
				poligons.add(outer);
				
				// Comprobar si tiene subpoligonos
				for (int y = 0; y < p.getNumInteriorRing(); y++)
					poligons.add(p.getInteriorRingN(y));
			}
		}
		else 
			System.out.println("["+new Timestamp(new Date().getTime())+"] Formato geometrico "+ 
		f.getDefaultGeometry().getClass().getName() +" desconocido dentro del shapefile PARCELA");

		// Inicializamos las listas
		this.nodes = new ArrayList<List<Long>>();
		this.ways = new ArrayList<List<Long>>();
		for(int x = 0; x < poligons.size(); x++){
			List<Long> lw = new ArrayList<Long>();
			List<Long> ln = new ArrayList<Long>();
			nodes.add(ln);
			ways.add(lw);
		}

		// Los demas atributos son metadatos y de ellos sacamos 
		refCatastral = (String) f.getAttribute("REFCAT");
		
		
		// El NUMSYMBOL 4 de las parcelas son las que a pesar de tener datos no se tienen que dibujar
		// como carreteras, la parcela rustica que cubre toda la zona urbana y alguna mas
		if (f.getAttribute("NUMSYMBOL") instanceof Double){
			numSymbol = (Integer) f.getAttribute("NUMSYMBOL");
		}
		else if (f.getAttribute("NUMSYMBOL") instanceof Long){
			numSymbol = (Integer) f.getAttribute("NUMSYMBOL");
		}
		else if (f.getAttribute("NUMSYMBOL") instanceof Integer){
			numSymbol = (Integer) f.getAttribute("NUMSYMBOL");
		}

		// Si queremos coger todos los atributos del .shp
		/*this.atributos = new ArrayList<ShapeAttribute>();
		for (int x = 1; x < f.getAttributes().size(); x++){
		atributos.add(new ShapeAttribute(f.getFeatureType().getDescriptor(x).getType(), f.getAttributes().get(x)));
		}*/
	}


	public String getShapeId(){
		return shapeId;
	}
	
	
	public void addNode(int pos, long nodeId){
		if (poligons.size()>pos)
			nodes.get(pos).add(nodeId);
	}

	
	public void addWay(int pos, long wayId){
		if (poligons.size()>pos && !ways.get(pos).contains(wayId))
			ways.get(pos).add(wayId);
	}


	public synchronized void deleteWay(int pos, long wayId){
		if (ways.size()>pos)
			ways.get(pos).remove(wayId);
	}

	
	public void setRelation(long relationId){
		relation = relationId;
	}

	
	public List<LineString> getPoligons(){
		return poligons;
	}

	
	/** Devuelve la lista de ids de nodos del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de nodos del poligono en posicion pos
	 */
	public synchronized List<Long> getNodesIds(int pos){
		if (nodes.size()>pos)
			return nodes.get(pos);
		else
			return null;
	}

	
	/** Devuelve la lista de ids de ways
	 * del poligono en posicion pos
	 * @param pos posicion que ocupa el poligono en la lista
	 * @return Lista de ids de ways del poligono en posicion pos
	 */
	public synchronized List<Long> getWaysIds(int pos) {
		if (nodes.size()>pos)
			return ways.get(pos);
		else
			return new ArrayList<Long>();
	}
	
	
	public Long getRelationId(){
		return relation;
	}

	
	public ShapeAttribute getAttribute(int x){	
		return atributos.get(x);
	}

	
	/** Devuelve los atributos del shape
	 * @return Lista de atributos
	 */
	public List<String[]> getAttributes(){
		List <String[]> l = new ArrayList<String[]>();
		String[] s = new String[2];

		if (refCatastral != null){
			s[0] = "catastro:ref"; s[1] = refCatastral;
			l.add(s);
		}

		s = new String[2];
		s[0] = "CAT2OSMSHAPEID"; s[1] = getShapeId();
		l.add(s);
		
		s = new String[2];
		s[0] = "source"; s[1] = "catastro";
		l.add(s);
		
		s = new String[2];
		s[0] = "source:date"; s[1] = Cat2OsmUtils.getFechaActual()+"";
		l.add(s);
		
		return l;
	}

	
	public String getRefCat(){
		return refCatastral;
	}

	
	public Coordinate[] getCoordenadas(int i){
		return poligons.get(i).getCoordinates();
	}

	
	public Coordinate getCoor(){
		return null;
	}
	
	
	public String getTtggss() {
		return null;
	}
	
	
	public boolean shapeValido (){
		 return (numSymbol != 4? true: false) ;
	}


	public HashMap<String,Double> getUsos() {
		return usos;
	}


	public void setUsos(HashMap<String,Double> usos) {
		this.usos = usos;
	}
	
	public void addUso(String cod, double area){
		if (usos == null)
			usos = new HashMap <String, Double>();
		
		if (usos.get(cod) == null)
			usos.put(cod, area);
		else{
			double a = usos.get(cod);
			a += area;
			usos.put(cod, a);
		}
	}
	
	public String getUsoMasArea(){
		
		if (usos == null)
			return "";
		
		String uso = "";
		double area = 0;
		Iterator<Entry<String, Double>> it = usos.entrySet().iterator();

		// Escribimos todos los nodos
		while(it.hasNext()){
			Map.Entry e = (Map.Entry)it.next();
			if ((Double)e.getValue() > area){
				area = (Double)e.getValue();
				uso = (String)e.getKey();
			}
		}
		return uso;
	}
	
}
