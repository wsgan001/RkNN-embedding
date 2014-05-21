package elkiTPL;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.evaluation.roc.ROC;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.AbstractRTreeSettings;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeNode;
import de.lmu.ifi.dbs.elki.persistent.LRUCache;
import de.lmu.ifi.dbs.elki.persistent.MemoryPageFile;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;


public class Simulation {
  final private static EuclideanDistanceFunction distanceFunction = EuclideanDistanceFunction.STATIC;;
  private static SpatialDistanceQuery distanceQuery;
  private static LRUCache c1;

  /**
   *
   * @param file
   * @param pageSize
   * @param k
   * @param dimension
   * @param withClipping
   * @return
   */
  public DistanceDBIDList<DoubleDistance> simulate(String file, int pageSize, int k, int dimension, boolean withClipping) {

    // create Memory Database
    Database[] db = createDatabase(file);
    Relation<DoubleVector> relation = db[0].getRelation(TypeUtil.NUMBER_VECTOR_FIELD);

    // create RStar Tree
    RStarTreeIndex dbIndex = createRStarTree(relation, pageSize);
    distanceQuery = distanceFunction.instantiate(relation);
    GenericTPLRkNNQuery gtpl = new GenericTPLRkNNQuery(dbIndex, distanceQuery, withClipping);

    // Generate random query point
//    double[] coordinates = new double[dimension];
//    for (int i = 0; i < dimension; i++){
//      coordinates[i] = Math.random();
//    }
//    DoubleVector queryObject = new DoubleVector(coordinates);
//    System.out.println("Generated query object: " + queryObject);

    // random query object from the database
    DoubleVector queryObject = relation.get(getDBObjectAsQueryObject(relation));
    System.out.println("Random query object from database: " + queryObject + "\n");

    // Performing rkNN Query
    System.out.println("Performing RkNN-Query...");
    long t0 = System.currentTimeMillis();

    DistanceDBIDList<DoubleDistance> rkNNs = gtpl.getRKNNForObject(queryObject, k);

    long t1 = System.currentTimeMillis();
    System.out.println("RkNN Query performed in " + (t1-t0) + " ms.\n");

    return rkNNs;
  }


  /**
   * Creates a file based database at given dbFilePath
   * @param dbFilePath Path where file based database should be stored
   * @return The database created
   */
  private Database[] createDatabase(String dbFilePath){

    ListParameterization parametrizationConfig = new ListParameterization();
    parametrizationConfig.addParameter(FileBasedDatabaseConnection.Parameterizer.INPUT_ID, dbFilePath); //synthetic data

    Database db = ClassGenericsUtil.parameterizeOrAbort(StaticArrayDatabase.class, parametrizationConfig);
    parametrizationConfig.failOnErrors();
    db.initialize();

    return new Database[]{db};
  }


  private RStarTreeIndex<DoubleVector> createRStarTree(Relation<DoubleVector> relation, int pageSize){
    System.out.println("Building R*-Tree... (Entries: " + relation.size() + ", page size: " + pageSize + " bytes)");

    long t0 = System.currentTimeMillis();

    PageFile<RStarTreeNode> memoryPageFile = new MemoryPageFile<RStarTreeNode>(pageSize);
    AbstractRTreeSettings settings = new AbstractRTreeSettings();
    RStarTreeIndex<DoubleVector> dbIndex = new RStarTreeIndex<DoubleVector>(relation, memoryPageFile, settings);
    dbIndex.initialize();

    long t1 = System.currentTimeMillis();

    System.out.println("R*-Tree built in " + (t1-t0) + " ms.");
    System.out.println("  objects: " + dbIndex.getRoot().getNumEntries() + "\n");

    return dbIndex;
  }


  /**
   * Generates a CSV file with "numPoints" random vectors in "dimensions" dimensions.
   * Example CSV: 0.3;0.22;0.9;
   * @param dimension
   * @param numPoints
   * @param csvPath
   * @throws IOException
   */
  public void generateCSVFile(int dimension, int numPoints, String csvPath) throws IOException {
    // create directories and file if non-existent
    Path pathToFile = Paths.get(csvPath);
    Files.createDirectories(pathToFile.getParent());
    if (!Files.exists(pathToFile))
        Files.createFile(pathToFile);
    FileWriter fw = new FileWriter(csvPath, false); // false = overwrite current file content
    BufferedWriter out = new BufferedWriter(fw);

    for (int i = 0; i < numPoints; i++){
      String newline = "";
      for (int j = 0; j < dimension; j++){
//        newline = newline + Math.random() + ";";
        NumberFormat doubleUKformatter = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.UK));
        newline = newline + doubleUKformatter.format(Math.random()) + ";";
      }
      if (i != 0){
        newline = "\n" + newline;
      }
      out.write(newline);
    }

    out.close();
  }

  /**
   * Gets a random object's DBID from the DB
   * @param relation The database relation
   * @return the DBID from the random object received from the DB
   */
  public DBID getDBObjectAsQueryObject(Relation<DoubleVector> relation){
    DBIDIter iter = relation.getDBIDs().iter();
    int randomNumber = (int) (Math.random() * relation.size() + 1);
    for (int i = 0; i < randomNumber && iter.valid(); i++){
      iter.advance();
    }

    return DBIDUtil.deref(iter);
  }
}