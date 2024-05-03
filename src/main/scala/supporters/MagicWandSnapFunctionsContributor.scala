package viper.silicion.supporters

import viper.silicon.Config
import viper.silicon.common.collections.immutable.InsertionOrderedSet
import viper.silicon.interfaces.{PreambleContributor, PreambleReader}
import viper.silicon.interfaces.decider.ProverLike
import viper.silicon.state.terms.{Sort, SortDecl, sorts}
import viper.silver.ast
import viper.silver.ast.Program

class MagicWandSnapFunctionsContributor(preambleReader: PreambleReader[String, String],
                                        config: Config)
  extends PreambleContributor[Sort, String, String] {

  private val FILE_DECLARATIONS = "/magic_wand_snap_functions_declarations.smt2"
  private val FILE_AXIOMS = "/magic_wand_snap_functions_axioms.smt2"
  private val FILE_AXIOMS_NO_TRIGGERS = "/magic_wand_snap_functions_axioms_no_triggers.smt2"

  private var collectedSorts: InsertionOrderedSet[Sort] = InsertionOrderedSet.empty
  private var collectedFunctionDecls: Iterable[String] = Seq.empty
  private var collectedAxioms: Iterable[String] = Seq.empty

  /* Lifetime */

  def reset(): Unit = {
    collectedSorts = InsertionOrderedSet.empty
    collectedFunctionDecls = Seq.empty
    collectedAxioms = Seq.empty
  }

  def stop(): Unit = {}

  def start(): Unit = {}

  /* Functionality */

  override def analyze(program: Program): Unit = {
    // If there are not magic wands, do not add any definitions or axioms
    if (!program.existsDefined { case ast.MagicWand(_, _) => true }) return

    collectedSorts = InsertionOrderedSet(sorts.MagicWandSnapFunction)
    collectedFunctionDecls = generateFunctionDecls
    collectedAxioms = generateAxioms
  }

  private def generateFunctionDecls: Iterable[String] =
    preambleReader.readPreamble(FILE_DECLARATIONS)

  private def generateAxioms: Iterable[String] =
    preambleReader.readPreamble(
      if (config.disableISCTriggers()) FILE_AXIOMS_NO_TRIGGERS else FILE_AXIOMS
    )

  override def sortsAfterAnalysis: Iterable[Sort] = collectedSorts

  override def declareSortsAfterAnalysis(sink: ProverLike): Unit = {
    sortsAfterAnalysis foreach (s => sink.declare(SortDecl(s)))
  }

  override def symbolsAfterAnalysis: Iterable[String] =
    extractPreambleLines(collectedFunctionDecls)

  override def declareSymbolsAfterAnalysis(sink: ProverLike): Unit =
    emitPreambleLines(sink, collectedFunctionDecls)

  override def axiomsAfterAnalysis: Iterable[String] =
    extractPreambleLines(collectedAxioms)

  override def emitAxiomsAfterAnalysis(sink: ProverLike): Unit =
    emitPreambleLines(sink, collectedAxioms)

  private def extractPreambleLines(from: Iterable[String]*): Iterable[String] =
    from.flatten

  private def emitPreambleLines(sink: ProverLike, from: Iterable[String]*): Unit = {
    from foreach { declarations =>
      sink.emit(declarations)
    }
  }
}
