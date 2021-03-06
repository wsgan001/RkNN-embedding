package app

import java.util.Date
import java.text.SimpleDateFormat

import app.Experiment.Experiment
import algorithms.{Eager, Naive, GraphRknn, Embedding}
import util.{RealTimeDiff, Log, Stats}
import util.Log.{experimentLogAppend, experimentLogAppendln}
import util.Utils.writeToFile

object RkNNTestEnvironment {

  val startDate = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss").format(new Date())

  /**
   * Run from console via:
   * java -jar "rknn.jar" <runs> <nrofquerypoints> <experimentsettings>
   * e.g.:
   * java -jar "rknn.jar" "5" "10" "experimentsettings.txt"

   * @param args
   */
  def main(args: Array[String]): Unit = {

    Log.appendln(s"###### Start: $startDate ######\n")
    Log.writeFlushWriteLog(appendToFile = false)
    writeToFile(s"log/${startDate}/experimentsLog.txt", false, "")

    try {
      runExperiments(
        short = false,
        runs  = 1,
        nrOfQueryPoints = 1
      )
//      runFromJar(
//        args = args
//      )
    }
    catch {
      case e: Exception => {
        Log.appendln(e.getStackTraceString)
        Log.writeFlushWriteLog(appendToFile = true)
        Log.printFlush
        e.printStackTrace
      }
    }
    Log.printFlush
    Log.writeFlushWriteLog(appendToFile = true)
  }

  def runExperiments(runs: Int, nrOfQueryPoints: Int, short: Boolean) {
    dryRun()

    experimentLogAppendln(s"--------------- Starting experiments.. -----------------\n")
    val runTimeRealTotal = RealTimeDiff()

    val naive     = Naive
    val eager     = Eager
    val embedding = Embedding
    val algorithms = Seq(naive, eager, embedding)


//    runExperiment(Experiment.RefPoints     , Seq(embedding), runs = runs, nrOfQueryPoints = nrOfQueryPoints, Seq(1,2,3,4,5,6,7,8,9,10),                                               short, Seq(5, 10, 15, 20))
//    runExperiment(Experiment.Default       , algorithms,     runs = runs, nrOfQueryPoints = nrOfQueryPoints, Seq(),                                                                   short, Seq())
//    runExperiment(Experiment.EntriesPerNode, Seq(embedding), runs = runs, nrOfQueryPoints = nrOfQueryPoints, Seq(1000,5000,10000,25000),                                 short, Seq(5, 10, 15, 20))
//    runExperiment(Experiment.Vertices      , algorithms,     runs = runs, nrOfQueryPoints = nrOfQueryPoints, Seq(1000, 10000, 50000, 100000, 200000, 350000),                         short, Seq(10,100,1000,10000))
//    runExperiment(Experiment.Connectivity  , algorithms,     runs = runs, nrOfQueryPoints = nrOfQueryPoints, Seq(0.001, 0.005, 0.01, 0.02, 0.04, 0.08, 0.1, 0.2, 0.4, 0.6, 0.8, 1.0), short, Seq(0.02, 0.04, 0.08, 0.16))
//    runExperiment(Experiment.ObjectDensity , algorithms,     runs = runs, nrOfQueryPoints = nrOfQueryPoints, Seq(0.001, 0.005, 0.01, 0.02, 0.04, 0.08, 0.1, 0.2, 0.4, 0.6, 0.8, 1.0), short, Seq(0.02, 0.04, 0.08, 0.16))
//    runExperiment(Experiment.K             , algorithms,     runs = runs, nrOfQueryPoints = nrOfQueryPoints, Seq(1, 2, 4, 8, 16, 20),                                                 short, Seq(2, 4, 8))

    runTimeRealTotal.end
    experimentLogAppendln(s"------------- All experiments finished in ${runTimeRealTotal}. ---------------\n")
  }


  def runFromJar(args: Array[String]) {

    if(args.length != 3)
      throw new IllegalArgumentException("Please pass an argument for <runs>, <nrofquerypoints>, and <experimentsettingsfile>")

    val runs            = Integer.parseInt(args(0))
    val nrOfQueryPoints = Integer.parseInt(args(1))
    val expSettingsFile = args(2)

    val source = scala.io.Source.fromFile(expSettingsFile)
    val experimentValuesTuples = source.getLines.toIndexedSeq.map(line => {val x = line.split(";"); (x(0).trim, x(1).split(",").map(_.trim).toSeq)})
    source.close()

    dryRun()

    experimentLogAppendln(s"--------------- Starting experiments.. -----------------\n")
    val runTimeRealTotal = RealTimeDiff()

    val naive     = Naive
    val eager     = Eager
    val embedding = Embedding
    val algorithms = Seq(naive, eager, embedding)

    experimentValuesTuples map { experimentValuesTuple => experimentValuesTuple match {
      case ("RefPoints"     , values) => runExperiment(Experiment.RefPoints     , Seq(embedding), runs = runs, nrOfQueryPoints = nrOfQueryPoints, expValues = values.map(Integer.parseInt(_)))
      case ("Default"       , values) => runExperiment(Experiment.Default       , algorithms,     runs = runs, nrOfQueryPoints = nrOfQueryPoints, expValues = Seq())
      case ("EntriesPerNode", values) => runExperiment(Experiment.EntriesPerNode, Seq(embedding), runs = runs, nrOfQueryPoints = nrOfQueryPoints, expValues = values.map(Integer.parseInt(_)))
      case ("Vertices"      , values) => runExperiment(Experiment.Vertices      , algorithms,     runs = runs, nrOfQueryPoints = nrOfQueryPoints, expValues = values.map(Integer.parseInt(_)))
      case ("Connectivity"  , values) => runExperiment(Experiment.Connectivity  , algorithms,     runs = runs, nrOfQueryPoints = nrOfQueryPoints, expValues = values.map(_.toDouble))
      case ("ObjectDensity" , values) => runExperiment(Experiment.ObjectDensity , algorithms,     runs = runs, nrOfQueryPoints = nrOfQueryPoints, expValues = values.map(_.toDouble))
      case ("K"             , values) => runExperiment(Experiment.K             , algorithms,     runs = runs, nrOfQueryPoints = nrOfQueryPoints, expValues = values.map(Integer.parseInt(_)))
      case _                          => throw new IllegalArgumentException("Illegal settings file")
      }
    }


    runTimeRealTotal.end
    experimentLogAppendln(s"------------- All experiments finished in ${runTimeRealTotal}. ---------------\n")
  }


  def runExperiment(experiment: Experiment, algorithms: Seq[GraphRknn], runs: Int, nrOfQueryPoints: Int, expValues: Seq[Any], short: Boolean = false, shortExpValues: Seq[Any] = Seq.empty) = {
    val values = if(short) shortExpValues else expValues

    experimentLogAppendln(s"${experiment.title} ($runs runs, $nrOfQueryPoints query points per run${if(short) ", short" else ""}${if(experiment != Experiment.Default) {s", ${experiment.valueName}: ${values mkString ", "}"} else ""})")
    experimentLogAppend(s"Generating ${if(values.isEmpty) 1 else values.size} x $runs graphs..")

    val realRunTimeExperiment = RealTimeDiff()
    val realRunTimeGraphGen   = RealTimeDiff()

    val setups: Seq[ExperimentSetup] =
      if(experiment == Experiment.Default)
        Seq(ExperimentSetup.default(runs = runs, nrOfQueryPoints = nrOfQueryPoints))
      else values map { value =>
        ExperimentSetup.forExperiment(
          experiment      = experiment,
          runs            = runs,
          nrOfQueryPoints = nrOfQueryPoints,
          experimentValue = value
        )
    }

    realRunTimeGraphGen.end
    experimentLogAppendln(s" done in $realRunTimeGraphGen\n", false)

    val experimentResult: ExperimentResult = new ExperimentResult(
      experiment                   = experiment,
      values                       = values,
      algorithmResultsForEachValue = Seq[Seq[AlgorithmResult]]()
    )

    experimentResult.write()

    for(setup <- setups){
      if(setup.experiment != Experiment.Default)
        experimentLogAppendln(s"  ${setup.experiment.valueName}: ${setup.experimentValue}")
      val algorithmResults: Seq[AlgorithmResult] = algorithms map (runExperimentForAlgorithm(setup, _))

      experimentResult.algorithmResultsForEachValue :+= algorithmResults
      experimentResult.write()
      experimentResult.appendDirectComparison()
      Log.experimentLog.append("\n")
    }

    realRunTimeExperiment.end
    experimentLogAppendln(s"Finished experiment in $realRunTimeExperiment.\n\n")
  }


  /**
   * For each graph in the given setup, run the given rknn algorithm
   * @param setup
   * @return AlgorithmResult
   */
  def runExperimentForAlgorithm(setup: ExperimentSetup, algorithm: GraphRknn): AlgorithmResult = {
    var nodesToRefine      = Seq[Int]()
    var nodesVisited       = Seq[Int]()
    var runTimeRknnQuery   = Seq[Int]()

    var embeddingFilteredCandidates        = Seq[Int]()
    var embeddingRunTimePreparation        = Seq[Int]()
    var embeddingRunTimeFilterRefEmbedding = Seq[Int]()
    var embeddingRunTimeRefinementOnGraph  = Seq[Int]()

    experimentLogAppend(s"  - ${algorithm.name}.. ")
    val realRunTimeAlgorithm = RealTimeDiff()

    for(((sGraph, q), i) <- setup.sGraphsQIds zipWithIndex) {
      Stats.reset()

      experimentLogAppend(s"${i+1} ", false)

      algorithm match {
        case Naive     => Naive.rknns(sGraph, q, setup.k)
        case Eager     => Eager.rknns(sGraph, q, setup.k)
        case Embedding => val (relation, rStarTree, dbidVertexIDMapping) = Embedding.createDatabaseWithIndex(sGraph, setup.numRefPoints, setup.rStarTreePageSize, s"filedatabase/${startDate}/rTree.csv")
                          val queryObject                                = Embedding.getQueryObject(relation, q, dbidVertexIDMapping)
                          Embedding.rknns(sGraph, q, setup.k, relation, queryObject, rStarTree, dbidVertexIDMapping)
      }

      nodesToRefine    :+= Stats.nodesToVerify
      nodesVisited     :+= Stats.nodesVisited
      runTimeRknnQuery :+= Stats.runTimeRknnQuery

      embeddingFilteredCandidates        :+= Stats.embeddingFilteredCandidates
      embeddingRunTimePreparation        :+= Stats.embeddingRunTimePreparation
      embeddingRunTimeFilterRefEmbedding :+= Stats.embeddingRunTimeFilterRefEmbedding
      embeddingRunTimeRefinementOnGraph  :+= Stats.embeddingRunTimeRefinementOnGraph
    }

    realRunTimeAlgorithm.end
    experimentLogAppendln(s" done in $realRunTimeAlgorithm", false)


    val singleResults = Seq[SingleResult](
      new SingleResult("Candidates to refine on graph"      , nodesToRefine),
      new SingleResult("Nodes visited"                      , nodesVisited),
      new SingleResult("Runtime thread CPU rknn query (ms.)", runTimeRknnQuery)
    ) ++ (
      algorithm match {
        case Embedding => Seq[SingleResult](
                            new SingleResult("Candidates left after filter"       ,       embeddingFilteredCandidates),
                            new SingleResult("Runtime embedding preparation (ms.)",       embeddingRunTimePreparation),
                            new SingleResult("Runtime filter refinement on R*Tree (ms.)", embeddingRunTimeFilterRefEmbedding),
                            new SingleResult("Runtime refinement on graph (ms.)",         embeddingRunTimeRefinementOnGraph)
                          )
        case _         => Nil
      }
    )

    new AlgorithmResult(algorithm.name, setup.runs, setup.nrOfQueryPoints, setup.experiment, singleResults)
  }

  /**
   *  Code-Organization: Perform all algorithms prior running the tests, so that the JVM can organize code.
   */
  def dryRun() = {
    val realRunTimeDryRun = new RealTimeDiff()
    experimentLogAppend("Code organization phase: Performing dry run (running all algorithms once)..")

    val setup = new ExperimentSetup(
      experiment          = null,
      experimentValue     = null,
      approximateVertices = 1000,
      objectDensity       = 0.05,
      connectivity        = 0.1,
      k                   = 3,
      numRefPoints        = 3,
      entriesPerNode      = 25,
      runs                = 1,
      nrOfQueryPoints     = 1
    )

    val (sGraph, q) = setup.sGraphsQIds.head

    Naive.rknns(sGraph, q, setup.k)
    Eager.rknns(sGraph, q, setup.k)
    val (relation, rStarTree, dbidVertexIDMapping) = Embedding.createDatabaseWithIndex(sGraph, setup.numRefPoints, setup.rStarTreePageSize, s"filedatabase/${startDate}/rTree.csv")
    val queryObject                                = Embedding.getQueryObject(relation, q, dbidVertexIDMapping)
    Embedding.rknns(sGraph, q, setup.k, relation, queryObject, rStarTree, dbidVertexIDMapping)

    realRunTimeDryRun.end
    experimentLogAppendln(s" done in $realRunTimeDryRun.\n\n", false)
  }
}