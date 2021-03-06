package org.allenai.pnp

import com.jayantkrish.jklol.tensor.Tensor
import com.jayantkrish.jklol.util.IndexedList
import com.jayantkrish.jklol.training.LogFunction
import com.jayantkrish.jklol.training.NullLogFunction

/** Mutable global state of a neural probabilistic program
  * execution. Env also tracks the chosen values for any
  * nondeterministic choices whose values depended on
  * computation graph nodes. These values are necessary
  * to compute gradients with respect to the neural network
  * parameters.
  *
  * Env is immutable.
  */
class Env(val labels: List[Tensor], val labelNodeIds: List[Int],
    varnames: IndexedList[String], vars: Array[AnyRef],
    val activeTimers: Set[String], val log: LogFunction) {

  /** Get the value of the named variable as an instance
    * of type A.
    */
  def getVar[A](name: String): A = {
    vars(varnames.getIndex(name)).asInstanceOf[A]
  }

  def getVar[A](nameInt: Int): A = {
    vars(nameInt).asInstanceOf[A]
  }

  /** Get a new environment with the named variable
    * set to value.
    */
  def setVar(name: String, value: AnyRef): Env = {
    val nextVarNames = if (varnames.contains(name)) {
      varnames
    } else {
      val i = IndexedList.create(varnames)
      i.add(name)
      i
    }

    val nextVars = Array.ofDim[AnyRef](nextVarNames.size)
    Array.copy(vars, 0, nextVars, 0, vars.size)
    val index = nextVarNames.getIndex(name)
    nextVars(index) = value

    new Env(labels, labelNodeIds, nextVarNames, nextVars, activeTimers, log)
  }

  def setVar(nameInt: Int, value: AnyRef): Env = {
    val nextVars = Array.ofDim[AnyRef](vars.size)
    Array.copy(vars, 0, nextVars, 0, vars.size)
    nextVars(nameInt) = value

    new Env(labels, labelNodeIds, varnames, nextVars, activeTimers, log)
  }

  def isVarBound(name: String): Boolean = {
    varnames.contains(name)
  }

  /** Attaches a label to a node of the computation graph in this
    * execution.
    */
  def addLabel(param: CompGraphNode, label: Tensor): Env = {
    new Env(label :: labels, param.id :: labelNodeIds, varnames, vars, activeTimers, log)
  }

  def setLog(newLog: LogFunction): Env = {
    new Env(labels, labelNodeIds, varnames, vars, activeTimers, newLog)
  }

  def startTimer(name: String): Env = {
    // log.startTimer(name)
    new Env(labels, labelNodeIds, varnames, vars, activeTimers + name, log)
  }

  def stopTimer(name: String): Env = {
    // log.stopTimer(name)
    new Env(labels, labelNodeIds, varnames, vars, activeTimers - name, log)
  }

  def pauseTimers(): Unit = {
    for (t <- activeTimers) {
      log.stopTimer(t)
    }
  }

  def resumeTimers(): Unit = {
    for (t <- activeTimers) {
      log.startTimer(t)
    }
  }
}

object Env {
  def init: Env = {
    new Env(List.empty, List.empty, IndexedList.create(), Array(),
      Set(), new NullLogFunction())
  }
}