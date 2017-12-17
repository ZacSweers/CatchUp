#!/usr/bin/env sh

# Because I always have to go look up how to do this
# First and only argument should be the auth token
if [[ $# -eq 0 ]] ; then
    echo 'Missing GitHub auth token argument!'
    exit 1
fi

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/schema.json"
curl -H "Authorization: bearer $1" https://api.github.com/graphql | python -m json.tool > ${DIR}
