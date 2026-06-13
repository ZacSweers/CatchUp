#!/usr/bin/env bash

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
CANONICAL_SCHEMA="${ROOT}/scripts/github/schema.json"
SERVICE_SCHEMA="${ROOT}/services/github/src/main/graphql/catchup/service/github/schema.json"
APP_SCHEMA="${ROOT}/app-scaffold/src/main/graphql/catchup/app/data/github/schema.json"
TMP_SCHEMA="$(mktemp)"

trap 'rm -f "${TMP_SCHEMA}"' EXIT

read -r -d '' INTROSPECTION_QUERY <<'GRAPHQL' || true
query IntrospectionQuery {
  __schema {
    queryType {
      name
    }
    mutationType {
      name
    }
    subscriptionType {
      name
    }
    types {
      ...FullType
    }
    directives {
      name
      description
      locations
      args {
        ...InputValue
      }
    }
  }
}

fragment FullType on __Type {
  kind
  name
  description
  fields(includeDeprecated: true) {
    name
    description
    args {
      ...InputValue
    }
    type {
      ...TypeRef
    }
    isDeprecated
    deprecationReason
  }
  inputFields {
    ...InputValue
  }
  interfaces {
    ...TypeRef
  }
  enumValues(includeDeprecated: true) {
    name
    description
    isDeprecated
    deprecationReason
  }
  possibleTypes {
    ...TypeRef
  }
}

fragment InputValue on __InputValue {
  name
  description
  type {
    ...TypeRef
  }
  defaultValue
}

fragment TypeRef on __Type {
  kind
  name
  ofType {
    kind
    name
    ofType {
      kind
      name
      ofType {
        kind
        name
        ofType {
          kind
          name
          ofType {
            kind
            name
            ofType {
              kind
              name
              ofType {
                kind
                name
              }
            }
          }
        }
      }
    }
  }
}
GRAPHQL

if ! command -v gh >/dev/null 2>&1; then
  echo "Missing GitHub CLI. Install gh and run 'gh auth login' first." >&2
  exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "Missing jq. Install jq before regenerating the GitHub schema." >&2
  exit 1
fi

gh api graphql -f query="${INTROSPECTION_QUERY}" \
  | jq '
      def normalize_default:
        if type == "string" then
          gsub("\"(?<value>[A-Z][A-Z0-9_]*)\""; .value)
        else
          .
        end;

      walk(
        if type == "object" and has("defaultValue") and .defaultValue != null then
          .defaultValue |= normalize_default
        else
          .
        end
      )
    ' > "${TMP_SCHEMA}"

jq empty "${TMP_SCHEMA}"

cp "${TMP_SCHEMA}" "${CANONICAL_SCHEMA}"
cp "${TMP_SCHEMA}" "${SERVICE_SCHEMA}"
cp "${TMP_SCHEMA}" "${APP_SCHEMA}"

echo "Updated GitHub GraphQL schemas:"
echo "  ${CANONICAL_SCHEMA}"
echo "  ${SERVICE_SCHEMA}"
echo "  ${APP_SCHEMA}"
