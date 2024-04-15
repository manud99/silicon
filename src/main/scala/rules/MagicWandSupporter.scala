// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.
//
// Copyright (c) 2011-2019 ETH Zurich.

package viper.silicon.rules

import viper.silver.ast
import viper.silver.ast.{Exp, Stmt}
import viper.silver.cfg.Edge
import viper.silver.cfg.silver.SilverCfg.SilverBlock
import viper.silver.verifier.PartialVerificationError
import viper.silicon._
import viper.silicon.decider.RecordedPathConditions
import viper.silicon.interfaces._
import viper.silicon.interfaces.state._
import viper.silicon.state._
import viper.silicon.state.terms.{MagicWandSnapshot, _}
import viper.silicon.utils.{freshSnap, toSf}
import viper.silicon.verifier.Verifier

object magicWandSupporter extends SymbolicExecutionRules {
  import consumer._
  import evaluator._
  import producer._

//  def checkWandsAreSelfFraming(s: State, g: Store, oldHeap: Heap, root: ast.Member, v: Verifier): VerificationResult =
//  {
//    val wands = Visitor.deepCollect(List(root), Nodes.subnodes){case wand: ast.MagicWand => wand}
//    var result: VerificationResult = Success()
//
//    breakable {
//      wands foreach {_wand =>
//        val err = MagicWandNotWellformed(_wand)
//
//        /* NOTE: Named wand, i.e. "wand w := A --* B", are currently not (separately) checked for
//         * self-framingness; instead, each such wand is replaced by "true --* true" (for the scope
//         * of the self-framingness checks implemented in this block of code).
//         * The reasoning here is that
//         *   (1) either A --* B is a wand that is actually used in the program, in which case
//         *       the other occurrences will be checked for self-framingness
//         *   (2) or A --* B is a wand that does not actually occur in the program, in which case
//         *       the verification will fail anyway
//         */
//        val trivialWand = (p: ast.Position) => ast.MagicWand(ast.TrueLit()(p), ast.TrueLit()(p))(p)
//        val wand = _wand.transform {
//          case v: ast.AbstractLocalVar if v.typ == ast.Wand => trivialWand(v.pos)
//        }()
//
//        val left = wand.left
//        val right = wand.withoutGhostOperations.right
//        val vs = Visitor.deepCollect(List(left, right), Nodes.subnodes){case v: ast.AbstractLocalVar => v}
//        val γ1 = Γ(vs.map(v => (v, fresh(v))).toIterable) + γ
//        val σ1 = Σ(γ1, Ø, g)
//
//        var σInner: S = null.asInstanceOf[S]
//
//        result =
//          locallyXXX {
//            produce(σ1, fresh, left, err, c)((σ2, c2) => {
//              σInner = σ2
//              Success()})
//          } && locallyXXX {
//            produce(σ1, fresh, right, err, c.copy(lhsHeap = Some(σInner.h)))((_, c4) =>
//              Success())}
//
//        result match {
//          case failure: Failure =>
//            /* Failure occurred. We transform the original failure into a MagicWandNotWellformed one. */
//            result = failure.copy(message = MagicWandNotWellformed(wand, failure.message.reason))
//            break()
//
//          case _: NonFatalResult => /* Nothing needs to be done*/
//        }
//      }
//    }
//
//    result
//  }

  def createChunk(s: State,
                  wand: ast.MagicWand,
                  snap: MagicWandSnapshot,
                  pve: PartialVerificationError,
                  v: Verifier)
                 (Q: (State, MagicWandChunk, Verifier) => VerificationResult)
                 : VerificationResult = {
    evaluateWandArguments(s, wand, pve, v)((s1, ts, v1) =>
      Q(s1, MagicWandChunk(MagicWandIdentifier(wand, s.program), s1.g.values, ts, snap, FullPerm), v1)
    )
  }

  /**
   * Evaluate all expressions inside the given magic wand instance in the current state.
   *
   * @param s State in which to expressions are evaluated.
   * @param wand Magic Wand instance.
   * @param Q Method whose second argument is used to return the evaluated terms of all expressions.
   */
  def evaluateWandArguments(s: State,
                            wand: ast.MagicWand,
                            pve: PartialVerificationError,
                            v: Verifier)
                           (Q: (State, Seq[Term], Verifier) => VerificationResult)
                           : VerificationResult = {
    val s1 = s.copy(exhaleExt = false)
    val es = wand.subexpressionsToEvaluate(s.program)

    evals(s1, es, _ => pve, v)((s2, ts, v1) => {
      Q(s2.copy(exhaleExt = s.exhaleExt), ts, v1)
    })
  }

  def consumeFromMultipleHeaps[CH <: Chunk]
                              (s: State,
                               hs: Stack[Heap],
                               pLoss: Term,
                               failure: Failure,
                               qvars: Seq[Var],
                               v: Verifier)
                              (consumeFunction: (State, Heap, Term, Verifier) => (ConsumptionResult, State, Heap, Option[CH]))
                              (Q: (State, Stack[Heap], Stack[Option[CH]], Verifier) => VerificationResult)
                              : VerificationResult = {

    val initialConsumptionResult = ConsumptionResult(pLoss, qvars, v, Verifier.config.checkTimeout())
      /* TODO: Introduce a dedicated timeout for the permission check performed by ConsumptionResult,
       *       instead of using checkTimeout. Reason: checkTimeout is intended for checks that are
       *       optimisations, e.g. detecting if a chunk provided no permissions or if a branch is
       *       infeasible. The situation is somewhat different here: the check should be time-bounded
       *       because not all permissions need to come from this stack, but the bound should be
       *       (significantly) higher to reduce the chances of missing a chunk that can provide
       *       permissions.
       */
    val initial = (initialConsumptionResult, s, Stack.empty[Heap], Stack.empty[Option[CH]])
    val (result, s1, heaps, consumedChunks) =
      hs.foldLeft[(ConsumptionResult, State, Stack[Heap], Stack[Option[CH]])](initial)((partialResult, heap) =>
        partialResult match  {
          case (r: Complete, sIn, hps, cchs)  => (r, sIn, heap +: hps, None +: cchs)
          case (Incomplete(permsNeeded), sIn, hps, cchs) =>
            val (success, sOut, h, cch) = consumeFunction(sIn, heap, permsNeeded, v)
            val tEq = (cchs.flatten.lastOption, cch) match {
              /* Equating wand snapshots would indirectly equate the actual left hand sides when they are applied
               * and thus be unsound. Since fractional wands do not exist it is not necessary to equate their
               * snapshots. Also have a look at the comments in the packageWand and applyWand methods.
               */
              case (Some(_: MagicWandChunk), Some(_: MagicWandChunk)) => True
              case (Some(ch1: NonQuantifiedChunk), Some(ch2: NonQuantifiedChunk)) => ch1.snap === ch2.snap
              case (Some(ch1: QuantifiedBasicChunk), Some(ch2: QuantifiedBasicChunk)) => ch1.snapshotMap === ch2.snapshotMap
              case _ => True
            }
            v.decider.assume(tEq)

            /* In the future it might be worth to recheck whether the permissions needed, in the case of
             * success being an instance of Incomplete, are zero.
             * For example if an assertion similar to x.f == 0 ==> acc(x.f) has previously been exhaled, Silicon
             * currently branches and if we learn that x.f != 0 from tEq above one of the branches becomes
             * infeasible. If a future version of Silicon would introduce conditionals to the permission term
             * of the corresponding chunk instead of branching we might get something similar to
             * Incomplete(W - (x.f == 0 ? Z : W)) for success, when using transfer to consume acc(x.f).
             * After learning x.f != 0 we would then be done, which is not detected by a smoke check.
             *
             * Note that when tEq is assumed it should be ensured, that permissions have actually been taken
             * from heap, i.e. that tEq does not result in already having the required permissions before
             * consuming from heap.
             */
            if (v.decider.checkSmoke()) {
              (Complete(), sOut, h +: hps, cch +: cchs)
            } else {
              (success, sOut, h +: hps, cch +: cchs)
            }
        })
    result match {
      case Complete() =>
        assert(heaps.length == hs.length)
        assert(consumedChunks.length == hs.length)
        Q(s1, heaps.reverse, consumedChunks.reverse, v)
      case Incomplete(_) => failure
    }
  }

  def packageWand(state: State,
                  wand: ast.MagicWand,
                  proofScript: ast.Seqn,
                  pve: PartialVerificationError,
                  v: Verifier)
                 (Q: (State, Chunk, Verifier) => VerificationResult)
                 : VerificationResult = {

    val s = if (state.exhaleExt) state else
      state.copy(reserveHeaps = Heap() :: state.h :: Nil)

    // v.logger.debug(s"wand = $wand")
    // v.logger.debug("c.reserveHeaps:")
    // s.reserveHeaps.map(v.stateFormatter.format).foreach(str => v.logger.debug(str, 2))

    val stackSize = 3 + s.reserveHeaps.tail.size
    // IMPORTANT: Size matches structure of reserveHeaps at [State RHS] below
    var results: Seq[(State, Stack[Term], Stack[Option[Exp]], Vector[RecordedPathConditions], Chunk)] = Nil

    /* TODO: When parallelising branches, some of the runtime assertions in the code below crash
     *       during some executions - since such crashes are hard to debug, branch parallelisation
     *       has been disabled for now.
     */
    val sEmp = s.copy(h = Heap(),
                      reserveHeaps = Nil,
                      exhaleExt = false,
                      conservedPcs = Vector[RecordedPathConditions]() +: s.conservedPcs,
                      recordPcs = true,
                      parallelizeBranches = false)

    def appendToResults(s5: State, ch: Chunk, pcs: RecordedPathConditions, v4: Verifier): Unit = {
      assert(s5.conservedPcs.nonEmpty, s"Unexpected structure of s5.conservedPcs: ${s5.conservedPcs}")

      var conservedPcs: Vector[RecordedPathConditions] = Vector.empty
      var conservedPcsStack: Stack[Vector[RecordedPathConditions]] = s5.conservedPcs

      // Producing a wand's LHS and executing the packaging proof code can introduce definitional path conditions, e.g.
      // new permission and snapshot maps, which are in general necessary to proceed after the
      // package statement, e.g. to know which permissions have been consumed.
      // Here, we want to keep *only* the definitions, but no other path conditions.

      conservedPcs = s5.conservedPcs.head :+ pcs.definitionsOnly

      conservedPcsStack =
        s5.conservedPcs.tail match {
          case empty @ Seq() => empty
          case head +: tail => (head ++ conservedPcs) +: tail
        }

      val s6 = s5.copy(conservedPcs = conservedPcsStack, recordPcs = s.recordPcs)

      results :+= (s6, v4.decider.pcs.branchConditions, v4.decider.pcs.branchConditionExps, conservedPcs, ch)
    }

    def createWandChunkAndRecordResults(s4: State,
                                        freshSnapRoot: Var,
                                        snap: Term,
                                        v3: Verifier)
                                       : VerificationResult = {
      val preMark = v3.decider.setPathConditionMark()
      v3.logger.debug(s"\npackageWand -> createWandChunkAndRecordResults: Create MagicWandSnapshot from freshSnapRoot $freshSnapRoot and snap $snap\n")

      // TODO: Find better solution to lift the definition of the magic wand snapshot into a wider scope.
      v3.decider.popScope()
      val wandSnapshot = createMagicWandSnapshot(freshSnapRoot, snap, v3)
      v3.decider.pushScope()

      // If the wand is part of a quantified expression
      if (s4.qpMagicWands.contains(MagicWandIdentifier(wand, s.program))) {
        val bodyVars = wand.subexpressionsToEvaluate(s.program)
        val formalVars = bodyVars.indices.toList.map(i => Var(Identifier(s"x$i"), v.symbolConverter.toSort(bodyVars(i).typ), false))

        evals(s4, bodyVars, _ => pve, v3)((s5, args, v4) => {
          val (sm, smValueDef) = quantifiedChunkSupporter.singletonSnapshotMap(s5, wand, args, MapLookup(wandSnapshot.wandMap, freshSnapRoot), v4)
          v4.decider.prover.comment("Definitional axioms for singleton-SM's value")
          v4.decider.assumeDefinition(smValueDef)
          val ch = quantifiedChunkSupporter.createSingletonQuantifiedChunk(formalVars, wand, args, FullPerm, sm, s.program)
          appendToResults(s5, ch, v4.decider.pcs.after(preMark), v4)
          Success()
        })

      } else {
        magicWandSupporter.createChunk(s4, wand, wandSnapshot, pve, v3)((s5, ch, v4) => {
          // v.logger.debug(s"done: create wand chunk: $ch")
          appendToResults(s5, ch, v4.decider.pcs.after(preMark), v4)
          Success()
        })
      }
    }

    val r = executionFlowController.locally(sEmp, v)((s1, v1) => {
      /* A snapshot (binary tree) will be constructed using First/Second datatypes,
       * that preserves the original root. The leafs of this tree will later appear
       * in the snapshot of the RHS at the appropriate places. Thus equating
       * `freshSnapRoot` with the snapshot received from consuming the LHS when
       * applying the wand preserves values from the LHS into the RHS.
       */
      val freshSnapRoot = freshSnap(sorts.Snap, v1)

      // Produce the wand's LHS.
      produce(s1.copy(conservingSnapshotGeneration = true), toSf(freshSnapRoot), wand.left, pve, v1)((sLhs, v2) => {
        val proofScriptCfg = proofScript.toCfg()

        /* Expected shape of reserveHeaps is either
         *   [hEmp, hOuter]
         * if we are executing a package statement (i.e. if we are coming from the executor), or
         *   [hEmp, hOps, ..., hOuterLHS, hOuter]
         * if we are executing a package ghost operation (i.e. if we are coming from the consumer).
         */
        val s2 = sLhs.copy(g = s.g, // TODO: s1.g? And analogously, s1 instead of s further down?
                           h = Heap(),
                           reserveHeaps = Heap() +: Heap() +: sLhs.h +: s.reserveHeaps.tail, /* [State RHS] */
                           reserveCfgs = proofScriptCfg +: sLhs.reserveCfgs,
                           exhaleExt = true,
                           oldHeaps = s.oldHeaps + (Verifier.MAGIC_WAND_LHS_STATE_LABEL -> sLhs.h),
                           conservingSnapshotGeneration = s.conservingSnapshotGeneration)
        /* s2.reserveHeaps is [hUsed, hOps, hLHS, ...], where hUsed and hOps are initially
         * empty, and where the dots represent the heaps belonging to surrounding package/packaging
         * operations. hOps will be populated while processing the RHS of the wand to package.
         * More precisely, each ghost operation (folding, applying, etc.) that is executed
         * populates hUsed during its execution. This is done by transferring permissions
         * from heaps lower in the stack, and by adding new chunks, e.g. a folded predicate.
         * Afterwards, it merges hUsed and hOps, which replaces hOps. hUsed is replaced by a
         * new empty heap. See also the final state updates in, e.g. method `applyingWand`
         * or `unfoldingPredicate` below.
         */
        assert(stackSize == s2.reserveHeaps.length)

        // v.logger.debug(s"done: produced LHS ${wand.left}")
        // v.logger.debug(s"next: consume RHS ${wand.right}")

        executor.exec(s2, proofScriptCfg, v2)((proofScriptState, proofScriptVerifier) => {
          consume(proofScriptState.copy(oldHeaps = s2.oldHeaps, reserveCfgs = proofScriptState.reserveCfgs.tail), wand.right, pve, proofScriptVerifier)((s3, snap, v3) => {

            // v.logger.debug(s"done: consumed RHS ${wand.right}")

            val s4 = s3.copy(//h = s.h, /* Temporarily */
                             exhaleExt = false,
                             oldHeaps = s.oldHeaps)

            // v.logger.debug(s"next: create wand chunk $freshSnapRoot")

            createWandChunkAndRecordResults(s4, freshSnapRoot, snap, v3)
          })
        })
      })
    })

    // v.logger.debug(s"\npackageWand -> results (${results.length}): $results\n")
    if (results.isEmpty) {
      // No results mean that packaging the wand resulted in inconsistent states on all paths,
      // and thus, that no wand chunk was created. In order to continue, we create one now.
      // Moreover, we need to set reserveHeaps to structurally match [State RHS] below.
      val s1 = sEmp.copy(reserveHeaps = Heap() +: Heap() +: Heap() +: s.reserveHeaps.tail)
      createWandChunkAndRecordResults(s1, freshSnap(sorts.Snap, v), freshSnap(sorts.Snap, v), v)
    }

    results.foldLeft(r)((res, packageOut) => {
      res && {
        val state = packageOut._1
        val branchConditions = packageOut._2
        val branchConditionsExp = packageOut._3
        val conservedPcs = packageOut._4
        val magicWandChunk = packageOut._5
        val s1 = state.copy(reserveHeaps = state.reserveHeaps.drop(3),
          parallelizeBranches = s.parallelizeBranches /* See comment above */
          /*branchConditions = c.branchConditions*/)
        executionFlowController.locally(s1, v)((s2, v1) => {
          v1.decider.setCurrentBranchCondition(And(branchConditions), Some(viper.silicon.utils.ast.BigAnd(branchConditionsExp.flatten)))
          conservedPcs.foreach(pcs => v1.decider.assume(pcs.conditionalized))
          Q(s2, magicWandChunk, v1)
        })
      }
    })
  }

  def applyWand(s: State,
                wand: ast.MagicWand,
                pve: PartialVerificationError,
                v: Verifier)
               (Q: (State, Verifier) => VerificationResult)
               : VerificationResult = {
    // Consume the magic wand instance "A --* B".
    consume(s, wand, pve, v)((s1, snap, v1) => {
      // Wrap snapshot inside MagicWandSnapshot class.
      // For now: assuming that snap is already a MagicWandSnapshot
      // TODO: handle situations where snapshot was inhaled and is not a MagicWandSnapshot yet.
      //  e.g. triggerWand.vpr -> snap = PredicateLookup(wand@0, sm@16@01, List(z@4@01, W, y@3@01, W))
      v.logger.debug(s"applyWand -> consume: snap = $snap")
      val wandSnap = MagicWandSnapshot(snap)

      // Consume the wand's LHS "A".
      consume(s1, wand.left, pve, v1)((s2, snap, v2) => {
        /* It is assumed that snap and wandSnap.abstractLhs are structurally the same.
         * Equating the two snapshots is sound iff a wand is applied only once.
         * Older solution in this case did use this assumption:
         * v2.decider.assume(snap === wandSnap.abstractLhs)
         */
        assert(snap.sort == sorts.Snap, s"expected snapshot but found: $snap")

        // Create copy of the state with a new labelled heap (i.e. `oldHeaps`) called "lhs".
        val s3 = s2.copy(oldHeaps = s1.oldHeaps + (Verifier.MAGIC_WAND_LHS_STATE_LABEL -> magicWandSupporter.getEvalHeap(s1)))

        // Produce the wand's RHS.
        produce(s3.copy(conservingSnapshotGeneration = true), toSf(MapLookup(wandSnap.wandMap, snap)), wand.right, pve, v2)((s4, v3) => {
          // Recreate old state without the magic wand, and the state with the oldHeap called lhs.
          val s5 = s4.copy(g = s1.g, conservingSnapshotGeneration = s3.conservingSnapshotGeneration)

          // Merge as many chunks as possible where we can deduce that they must be aliases
          // from their permissions, or add permissions that we can infer.
          // Afterward also remove labelled old heap "lhs".
          val s6 = v3.stateConsolidator(s5).consolidate(s5, v3).copy(oldHeaps = s1.oldHeaps)

          Q(s6, v3)
        })
      })
    })
  }

  def transfer[CH <: Chunk]
              (s: State,
               perms: Term,
               failure: Failure,
               qvars: Seq[Var],
               v: Verifier)
              (consumeFunction: (State, Heap, Term, Verifier) => (ConsumptionResult, State, Heap, Option[CH]))
              (Q: (State, Option[CH], Verifier) => VerificationResult)
              : VerificationResult = {
    assert(s.recordPcs)
    /* During state consolidation or the consumption of quantified permissions new chunks with new snapshots
     * might be created, the information about these new snapshots is stored in the path conditions and needs
     * to be preserved after the package operation finishes.
     * It is assumed that only information regarding snapshots is added to the path conditions during the
     * execution of the consumeFunction. If any other assumptions from the wand's lhs or footprint are
     * recorded, this might not be sound! This might especially happen when consumeFromMultipleHeaps is
     * called in an inconsistent state or when transfer results in an inconsistent state. One solution to
     * consider might be to store the conserved path conditions in the wand's chunk and restore them during
     * the apply operation.
     */
    val preMark = v.decider.setPathConditionMark()
    executionFlowController.tryOrFail2[Stack[Heap], Stack[Option[CH]]](s, v)((s1, v1, QS) =>
      magicWandSupporter.consumeFromMultipleHeaps(s1, s1.reserveHeaps.tail, perms, failure, qvars, v1)(consumeFunction)(QS)
    )((s2, hs2, chs2, v2) => {
      val conservedPcs = s2.conservedPcs.head :+ v2.decider.pcs.after(preMark)
      val s3 = s2.copy(conservedPcs = conservedPcs +: s2.conservedPcs.tail, reserveHeaps = s.reserveHeaps.head +: hs2)

      val usedChunks = chs2.flatten
      val (fr4, hUsed) = v2.stateConsolidator(s2).merge(s3.functionRecorder, s2.reserveHeaps.head, Heap(usedChunks), v2)

      val s4 = s3.copy(functionRecorder = fr4, reserveHeaps = hUsed +: s3.reserveHeaps.tail)

      /* Returning the last of the usedChunks should be fine w.r.t to the snapshot
       * of the chunk, since consumeFromMultipleHeaps should have equated the
       * snapshots of all usedChunks, except for magic wand chunks, where usedChunks
       * is potentially a series of empty chunks (perm = Z) followed by the that was
       * actually consumed.
       */
      Q(s4, usedChunks.lastOption, v2)})
  }

  def getEvalHeap(s: State): Heap = {
    if (s.exhaleExt) {
      /* s.reserveHeaps = [hUsed, hOps, sLhs, ...]
       * After a proof script statement such as fold has been executed, hUsed is empty and
       * hOps contains the chunks that were either transferred or newly produced by
       * the statement. Evaluating an expression, e.g. predicate arguments of
       * a subsequent fold, thus potentially requires chunks from hOps.
       * Such an expression should also be able to rely on permissions gained from the lhs
       * of the wand, i.e. chunks in sLhs.
       * On the other hand, once the innermost assertion of the RHS of a wand is
       * reached, permissions are transferred to hUsed, and expressions of the innermost
       * assertion therefore potentially require chunks from hUsed.
       * Since innermost assertions must be self-framing, combining hUsed, hOps and hLhs
       * is sound.
       */
      s.reserveHeaps.head + s.reserveHeaps(1) + s.reserveHeaps(2)
    } else
      s.h
  }

  def getExecutionHeap(s: State): Heap =
    if (s.exhaleExt) s.reserveHeaps.head
    else s.h

  def moveToReserveHeap(newState: State, v: Verifier): State =
    if (newState.exhaleExt) {
      /* newState.reserveHeaps = [hUsed, hOps, ...]
       * During execution permissions are consumed or transferred from hOps and new
       * ones are generated onto the state's heap. E.g. for a fold the body of a predicate
       * is consumed from hOps and permissions for the predicate are added to the state's
       * heap. After a statement is executed those permissions are transferred to hOps.
       */
      val (fr, hOpsJoinUsed) = v.stateConsolidator(newState).merge(newState.functionRecorder, newState.reserveHeaps(1), newState.h, v)
      newState.copy(functionRecorder = fr, h = Heap(),
          reserveHeaps = Heap() +: hOpsJoinUsed +: newState.reserveHeaps.drop(2))
    } else newState

  def getOutEdges(s: State, b: SilverBlock): Seq[Edge[Stmt, Exp]] =
    if (s.exhaleExt)
      s.reserveCfgs.head.outEdges(b)
    else
      s.methodCfg.outEdges(b)

  /**
   * Define wand for all possible snapshots.
   *
   * @param abstractLhs Logic variable representing the abstract left hand side of the wand.
   * @param rhsSnapshot Snapshot of the right hand side of the wand which implements values from the left hand side at the appropriate places.
   * @param v Verifier with which a variable is created in the current context.
   * @return The snapshot of the wand.
   */
  def createMagicWandSnapshot(abstractLhs: Var, rhsSnapshot: Term, v: Verifier): MagicWandSnapshot = {
    v.decider.prover.comment("Create new magic wand snapshot map")

    // Create Map that takes a snapshot, which represent the values of the consumed LHS of the wand,
    // and relates it to the snapshot of the RHS. We use this to preserve values of the LHS in the RHS snapshot.
    val wandMap = v.decider.fresh("$wm", sorts.Map(sorts.Snap, sorts.Snap))
    v.decider.assume(Forall(abstractLhs, MapLookup(wandMap, abstractLhs) === rhsSnapshot, Trigger(MapLookup(wandMap, abstractLhs))))

    MagicWandSnapshot(wandMap)
  }
}
