#
# If not stated otherwise in this file or this component's LICENSE file the
# following copyright and licenses apply:
#
# Copyright 2023 Liberty Global Technology Services BV
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

{{- define "appstore-bundle-charts.name" -}}
{{- .Chart.Name | trunc 40 | trimSuffix "-" -}}
{{- end -}}

{{- define "appstore-bundle-charts.appSuffix" -}}
{{- $globalAppSuffix := "" -}}
{{- if not (empty .Values.appSuffix) -}}
{{- $globalAppSuffix = .Values.appSuffix -}}
{{- else if not (empty .Values.global) -}}
    {{- if not (empty .Values.global.appSuffix) -}}
        {{- $globalAppSuffix = .Values.global.appSuffix -}}
    {{- end -}}
{{- end -}}
{{- $globalAppSuffix -}}
{{- end -}}

{{- define "appstore-bundle-charts.fullname" -}}
{{- $appSuffix := include "appstore-bundle-charts.appSuffix" . -}}
{{- printf "%s-%s" .Chart.Name $appSuffix| trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "appstore-bundle-charts.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}
