package io.sweers.catchup.serviceregistry

import io.sweers.catchup.serviceregistry.annotations.Meta
import io.sweers.catchup.serviceregistry.annotations.ServiceRegistry

@ServiceRegistry
interface CatchUpServiceRegistry

@Meta
@ServiceRegistry
interface CatchUpServiceMetaRegistry
