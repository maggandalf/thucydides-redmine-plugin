when 'In Progress', {
    'success' should: 'Resolved'
}

when 'Resolved', {
    'failure' should: 'In Progress'
}

when 'Closed', {
	'failure' should: 'In Progress'
}