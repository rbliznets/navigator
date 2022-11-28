package ru.glorient.granitbk_n.camera.opengl.machines

interface Action

interface State

interface StateMachine<S : State, A : Action> {
    var state: S
    fun transition(action: A, id: Int)
    fun send(action: A)
}