{{/*
Common labels attached to every resource in this chart.
*/}}
{{- define "baemin.labels" -}}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
{{- end }}
