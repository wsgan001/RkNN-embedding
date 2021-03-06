package app

import scala.util.Random
import graph.{SVertex, SGraph, GraphGen}
import ExperimentSetup._
import scala.collection.immutable.HashMap
import app.Experiment.Experiment


object ExperimentSetup {

  implicit def double2Int(d: Double) = d.toInt

  def default: ExperimentSetup = default()
  def default(experiment         : Experiment = Experiment.Default,
              experimentValue    : Any        = null,
              entriesPerNode     : Int        = 25,
              runs               : Int        = 5,
              numRefPoints       : Int        = 15,
              approximateVertices: Int        = 50000,
              objectDensity      : Double     = 0.02,
              connectivity       : Double     = 0.3,
              k                  : Int        = 4,
              nrOfQueryPoints    : Int        = 10): ExperimentSetup = {
      ExperimentSetup(
        experiment,
        experimentValue,
        entriesPerNode,
        runs,
        numRefPoints,
        approximateVertices,
        objectDensity,
        connectivity,
        k,
        nrOfQueryPoints
      )
  }

  def forExperiment(experiment: Experiment, runs: Int, nrOfQueryPoints: Int, experimentValue: Any) = {
    experiment match {
      case Experiment.Default        => ExperimentSetup.default(experiment = experiment, runs = runs, nrOfQueryPoints = nrOfQueryPoints)
      case Experiment.EntriesPerNode => ExperimentSetup.default(experiment = experiment, runs = runs, nrOfQueryPoints = nrOfQueryPoints, entriesPerNode      = experimentValue.asInstanceOf[Int]   , experimentValue = experimentValue)
      case Experiment.RefPoints      => ExperimentSetup.default(experiment = experiment, runs = runs, nrOfQueryPoints = nrOfQueryPoints, numRefPoints        = experimentValue.asInstanceOf[Int]   , experimentValue = experimentValue)
      case Experiment.Vertices       => ExperimentSetup.default(experiment = experiment, runs = runs, nrOfQueryPoints = nrOfQueryPoints, approximateVertices = experimentValue.asInstanceOf[Int]   , experimentValue = experimentValue)
      case Experiment.ObjectDensity  => ExperimentSetup.default(experiment = experiment, runs = runs, nrOfQueryPoints = nrOfQueryPoints, objectDensity       = experimentValue.asInstanceOf[Double], experimentValue = experimentValue)
      case Experiment.Connectivity   => ExperimentSetup.default(experiment = experiment, runs = runs, nrOfQueryPoints = nrOfQueryPoints, connectivity        = experimentValue.asInstanceOf[Double], experimentValue = experimentValue)
      case Experiment.K              => ExperimentSetup.default(experiment = experiment, runs = runs, nrOfQueryPoints = nrOfQueryPoints, k                   = experimentValue.asInstanceOf[Int]   , experimentValue = experimentValue)
    }
  }

}

/**
 *
 * @param vertices        // e.g. 1000        Max. 1 Million! (for no integer overflow for max-edges)
 * @param objectDensity   // recom.: 0.05
 * @param connectivity    // recom.: 0.1
 * @param k               // e.g. 4
 * @param numRefPoints    // e.g. 6
 * @param entriesPerNode  // recom.: 25
 */
case class ExperimentSetup(experiment         : Experiment,
                           experimentValue    : Any,
                           entriesPerNode     : Int,
                           runs               : Int,
                           numRefPoints       : Int,
                           approximateVertices: Int,
                           objectDensity      : Double,
                           connectivity       : Double,
                           k                  : Int,
                           nrOfQueryPoints    : Int){

  val vertices              = Math.pow(Math.sqrt(approximateVertices).floor, 2).toInt
  val objects               = if (objectDensity * vertices <= 1) 2 else (objectDensity * vertices).ceil.toInt
  val nrOfRowsAndCols       = Math.sqrt(vertices)
  val rowEdges              = nrOfRowsAndCols * (nrOfRowsAndCols - 1)
  val minEdges              = rowEdges + (nrOfRowsAndCols - 1)
  val maxEdges              = (nrOfRowsAndCols - 1) * 2 * nrOfRowsAndCols // Maximum: for n rows: 2n edges.    // (Math.pow(nrOfRowsAndCols, 2))    // Maximum Edges: all edges between all rows: cols * (rows - 1)
  val edges                 = (connectivity * (maxEdges - minEdges) + minEdges).toInt   // generally for a graph: from N-1 to N(N-1)/2 // Int Overflow at: max 2.147.483.647 => Vertex max: 65.536
  val rStarTreePageSize     = (entriesPerNode * numRefPoints * 16) + 34

  val sGraphsQIds: Seq[(SGraph, SVertex)] = {
    val differentGraphs = for {_ <- 1 to runs} yield {      // generate 'runs' different graphs
      GraphGen.generateScalaGraph(vertices, edges, objects)
    }

    val graphs = differentGraphs flatMap {graph =>          // duplicate each graph by 'nrOfQueryPoints'
      for {_ <- 1 to nrOfQueryPoints}
      yield {
        graph
      }
    }

    graphs map { graph =>                                   // for each graph create a query point
      val q      = graph.getVertex(new Random(System.currentTimeMillis).nextInt(vertices))
      if (!q.containsObject)
        q.setObjectId(vertices)
      (graph, q)
    }
  }

  override def toString = {
    (HashMap[Experiment, String](
      Experiment.ObjectDensity  -> s"Object Density:          $objectDensity",
      Experiment.Vertices       -> s"Vertices:                $approximateVertices",
      Experiment.Connectivity   -> s"Connectivity:            $connectivity",
      Experiment.K              -> s"k:                       $k",
      Experiment.RefPoints      -> s"Reference points:        $numRefPoints",
      Experiment.EntriesPerNode -> s"Entries per R*Tree node: $entriesPerNode") - experiment).values mkString ("\n")
  }
}
