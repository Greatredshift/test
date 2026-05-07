package com.beatmachine.sp404.audio.effects

import com.beatmachine.sp404.model.EffectType

abstract class Effect(val type: EffectType, protected val sampleRate: Int) {
    var enabled: Boolean = true
    var dryWet: Float = 1.0f    // 0 = dry, 1 = wet
    val params: MutableMap<String, Float> = mutableMapOf()

    abstract fun process(buffer: FloatArray)
    open fun reset() {}

    protected fun param(key: String, default: Float) = params.getOrPut(key) { default }
    fun setParam(key: String, value: Float) { params[key] = value }

    protected fun softClip(x: Float): Float {
        return when {
            x > 1f -> 1f
            x < -1f -> -1f
            else -> x - (x * x * x) / 3f
        }
    }

    protected fun dcBlock(x: Float, prev: Float, prevOut: Float): Float {
        return x - prev + 0.995f * prevOut
    }
}

class EffectChain(private val sampleRate: Int) {
    private val effects = mutableListOf<Effect>()

    fun add(effect: Effect) = effects.add(effect)
    fun remove(effect: Effect) = effects.remove(effect)
    fun clear() = effects.clear()
    fun getEffects(): List<Effect> = effects

    fun process(buffer: FloatArray) {
        val dry = FloatArray(buffer.size) { buffer[it] }
        for (effect in effects) {
            if (effect.enabled) {
                if (effect.dryWet < 1f) {
                    val wet = buffer.copyOf()
                    effect.process(wet)
                    val w = effect.dryWet
                    val d = 1f - w
                    for (i in buffer.indices) buffer[i] = dry[i] * d + wet[i] * w
                } else {
                    effect.process(buffer)
                }
            }
        }
    }

    fun reset() = effects.forEach { it.reset() }
}
