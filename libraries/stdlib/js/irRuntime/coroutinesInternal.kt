/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

import kotlin.coroutines.experimental.*

internal external fun <T> getContinuation(): Continuation<T>
internal external suspend fun <T> returnIfSuspended(@Suppress("UNUSED_PARAMETER") argument: Any?): T

fun <T> normalizeContinuation(continuation: Continuation<T>): Continuation<T> =
    (continuation as? CoroutineImpl)?.facade ?: continuation

internal fun <T> interceptContinuationIfNeeded(
    context: CoroutineContext,
    continuation: Continuation<T>
) = context[ContinuationInterceptor]?.interceptContinuation(continuation) ?: continuation


@SinceKotlin("1.2")
@Suppress("WRONG_MODIFIER_TARGET")
public suspend  val coroutineContext: CoroutineContext
    get() {
        throw Exception("Implemented as intrinsic")
    }


internal abstract class CoroutineImpl(private val completion: Continuation<Any?>) : Continuation<Any?> {
    protected var exceptionState = 0
    protected var label: Int = 0

    protected var pendingException: dynamic = null

    public override val context: CoroutineContext get() = completion?.context

    val facade: Continuation<Any?> get() {
        return if (context != null) interceptContinuationIfNeeded(context, this)
        else this
    }

    override fun resume(value: Any?) {
        doResumeWrapper(value, null)
    }

    override fun resumeWithException(exception: Throwable) {
        label = exceptionState
        pendingException = exception
        doResumeWrapper(null, exception)
    }

    protected fun doResumeWrapper(data: Any?, exception: Throwable?) {
        processBareContinuationResume(completion) { doResume(data, exception) }
    }

    protected abstract fun doResume(data: Any?, exception: Throwable?): Any?

    open fun create(completion: Continuation<*>): Continuation<Unit> {
        throw IllegalStateException("create(Continuation) has not been overridden")
    }

    open fun create(value: Any?, completion: Continuation<*>): Continuation<Unit> {
        throw IllegalStateException("create(Any?;Continuation) has not been overridden")
    }
}