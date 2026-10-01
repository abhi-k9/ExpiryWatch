package io.github.abhik9.expirywatch.core.diagnostics.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import dagger.multibindings.Multibinds
import io.github.abhik9.expirywatch.core.common.diagnostics.DiagnosticsSection
import io.github.abhik9.expirywatch.core.common.diagnostics.EventLog
import io.github.abhik9.expirywatch.core.diagnostics.AppDiagnosticsSection
import io.github.abhik9.expirywatch.core.diagnostics.DeviceDiagnosticsSection
import io.github.abhik9.expirywatch.core.diagnostics.DiagnosticsLog
import io.github.abhik9.expirywatch.core.diagnostics.DiagnosticsReportRepository
import io.github.abhik9.expirywatch.core.diagnostics.ProcessExitsDiagnosticsSection
import io.github.abhik9.expirywatch.core.domain.repository.DiagnosticsRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DiagnosticsModule {
    @Binds
    abstract fun bindsEventLog(log: DiagnosticsLog): EventLog

    @Binds
    abstract fun bindsDiagnosticsRepository(repository: DiagnosticsReportRepository): DiagnosticsRepository

    /** Other modules describe their own state by adding sections to this set. */
    @Multibinds
    abstract fun sections(): Set<DiagnosticsSection>

    @Binds
    @IntoSet
    abstract fun bindsAppSection(section: AppDiagnosticsSection): DiagnosticsSection

    @Binds
    @IntoSet
    abstract fun bindsDeviceSection(section: DeviceDiagnosticsSection): DiagnosticsSection

    @Binds
    @IntoSet
    abstract fun bindsProcessExitsSection(section: ProcessExitsDiagnosticsSection): DiagnosticsSection
}
