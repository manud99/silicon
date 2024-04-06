// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// Copyright (c) 2011-2024 ETH Zurich.

package viper.silicon.supporters

import com.typesafe.scalalogging.Logger
import viper.silver.ast
import viper.silver.ast.utility.Functions
import viper.silver.components.StatefulComponent
import viper.silver.verifier.errors.{ContractNotWellformed, FunctionNotWellformed, PostconditionViolated}
import viper.silicon.{Config, Map, Stack, toMap}
import viper.silicon.interfaces.decider.ProverLike
import viper.silicon.interfaces._
import viper.silicon.state._
import viper.silicon.state.State.OldHeaps
import viper.silicon.state.terms._
import viper.silicon.state.terms.predef.`?s`
import viper.silicon.common.collections.immutable.InsertionOrderedSet
import viper.silicon.decider.Decider
import viper.silicon.rules.{consumer, evaluator, executionFlowController, producer}
import viper.silicon.supporters.PredicateData
import viper.silicon.verifier.{Verifier, VerifierComponent}
import viper.silicon.utils.{freshSnap, toSf}
import viper.silicon.utils.ast.ViperEmbedding
import viper.silver.ast.{AbstractLocalVar, AccessPredicate, Applying, BinExp, CondExp, DomainAxiom, DomainFunc, Exists, ExtensionExp, ForPerm, Forall, ForbiddenInTrigger, FuncLikeApp, InhaleExhaleExp, Lhs, LocationAccess, MagicWand, MapExp, MultisetExp, PermExp, PossibleTrigger, Program, QuantifiedExp, ResourceAccess, SeqExp, SetExp, TypeVar, UnExp, Unfolding}

import scala.collection.immutable.{ListSet, Map}
import scala.reflect.{ClassTag, classTag}

class DefaultMagicWandContributor(override val domainTranslator: DomainsTranslator[Term], config: Config)
  extends DefaultMapsContributor(domainTranslator, config)
    with ExpressionTranslator {

  private var collectedSorts: InsertionOrderedSet[Sort] = InsertionOrderedSet.empty
  private var collectedFunctions = InsertionOrderedSet[DomainFun]()
  private var collectedAxioms = InsertionOrderedSet[Term]()

  override def reset(): Unit = {
    collectedSorts = InsertionOrderedSet.empty
    collectedFunctions = InsertionOrderedSet.empty
    collectedAxioms = InsertionOrderedSet.empty
  }

  // TODO: Use analyze to only add these definitions when there is a magic wand packed in a program
  override def analyze(program: Program): Unit = {
    val sourceProgram = loadProgramFromUrl(sourceUrl)
    val sourceDomain = transformSourceDomain(sourceProgram.findDomain(sourceDomainName))

    val instantiation = sourceDomain.typVars.zip(List(ViperEmbedding(sorts.Snap), ViperEmbedding(sorts.Snap))).toMap
    val typeInstances = ListSet(ast.DomainType(sourceDomain, instantiation))
    val instantiationsWithType = instantiateWithDomain(sourceProgram, sourceDomain, typeInstances)

    collectSorts(typeInstances)
    collectFunctions(instantiationsWithType, program)
    collectAxioms(instantiationsWithType)
    // val sourceDomainTypeInstances =
    //   builtinDomainTypeInstances map (builtinTypeInstance => {
    //     val instantiation : Map[viper.silver.ast.TypeVar,viper.silver.ast.Type] = sourceDomain.typVars.zip(builtinTypeInstance.typeArguments).toMap
    //     //(instantiation, ast.DomainType(sourceDomain, instantiation))
    //     ast.DomainType(sourceDomain, instantiation)
    //   })

    /*
    // Convert generic functions to functions which take $Snap as input and output
    val functions = sourceDomain.functions.map((function: DomainFunc) => {
      val inSorts = function.formalArgs.map(_.typ).map {
        case ast.MapType(_, _) => sorts.Map(sorts.Snap, sorts.Snap)
        case _ => sorts.Snap
      }
      val outSort = sorts.Snap
      symbolConverter.toFunction(function, inSorts :+ outSort, program)
    })
    collectedFunctions = InsertionOrderedSet(functions)

    // TODO: Convert all axioms to take $Snap arguments
    val axioms = sourceDomain.axioms.map((axiom: DomainAxiom) => {
      axiom.exp match {
        case predicate: AccessPredicate => ???
        case InhaleExhaleExp(in, ex) => ???
        case exp: PermExp => ???
        case access: LocationAccess => ???
        case access: ResourceAccess => ???
        case CondExp(cond, thn, els) => ???
        case Unfolding(acc, body) => ???
        case Applying(wand, body) => ???
        case ast.Let(variable, exp, body) => ???
        case exp: QuantifiedExp => {
          exp match {
            case Forall(vars, triggers, body) => {
              terms.Quantification(terms.Forall,
                vars.map(_.localVar) map (v => Var(Identifier(v.name), sorts.Snap, false)),
                translate(symbolConverter.toSort)(body),
                triggers map ((triggerSet: ast.Trigger) => terms.Trigger(triggerSet.exps map (trigger => translate(symbolConverter.toSort)(trigger)))),
                "",
                isGlobal = false,
                None
              )
            }
            case Exists(variables, triggers, exp) => ???
            case ForPerm(variables, resource, body) => ???
          }
        }
        case ForPerm(variables, resource, body) => ???
        case localVar: AbstractLocalVar => ???
        case exp: SeqExp => ???
        case exp: SetExp => ???
        case exp: MultisetExp => ???
        case exp: MapExp => ???
        case literal: ast.Literal => ???
        case trigger: PossibleTrigger => ???
        case trigger: ForbiddenInTrigger => ???
        case app: FuncLikeApp => ???
        case exp: BinExp => ???
        case exp: UnExp => ???
        case lhs: Lhs => ???
        case exp: ExtensionExp => ???
      }
    })
    */
    assert(true)
  }

  // override def sortsAfterAnalysis: InsertionOrderedSet[Sort] = collectedSorts
  // override def sortsAfterAnalysis: InsertionOrderedSet[Sort] = InsertionOrderedSet(Seq(sorts.Set(sorts.Snap), sorts.Map(sorts.Snap, sorts.Snap)))

  // override def declareSortsAfterAnalysis(sink: ProverLike): Unit = {
  //   sortsAfterAnalysis foreach (s => sink.declare(SortDecl(s)))
  // }

  // override def symbolsAfterAnalysis: InsertionOrderedSet[DomainFun] = collectedFunctions

  // override def declareSymbolsAfterAnalysis(sink: ProverLike): Unit = {
  //   symbolsAfterAnalysis foreach (f => sink.declare(FunctionDecl(f)))
  // }

  // override def axiomsAfterAnalysis: Iterable[Term] = collectedAxioms

  // override def emitAxiomsAfterAnalysis(sink: ProverLike): Unit = ()

  override def start(): Unit = {}

  override def stop(): Unit = {}

  // override type BuiltinDomainType = ast.MapType

  // val builtinDomainTypeTag: ClassTag[BuiltinDomainType] = classTag[ast.MapType]

  // override def defaultSourceResource: String = "/dafny_axioms/maps.vpr"

  // override def userProvidedSourceFilepath: Option[String] = config.mapAxiomatizationFile.toOption

  // override def sourceDomainName: String = "$Map"

  // override def targetSortFactory(argumentSorts: Iterable[Sort]): Sort = {
  //   assert(argumentSorts.size == 2)
  //   sorts.Map(argumentSorts.head, argumentSorts.tail.head)
  // }
}
