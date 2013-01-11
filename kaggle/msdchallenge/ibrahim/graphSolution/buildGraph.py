import networkx as nx
import itertools
import fileinput

G = nx.Graph()

""" Adds an edge w/ weight 1, increments if already exists
    Uses graph G if left unspecified"""
def addNode(node1, node2, iWeight=1, iG=G):
    if iG.has_edge(node1, node2):
        iG[node1][node2]['weight'] += iWeight
    else:
        iG.add_edge(node1,node2, weight = iWeight)

""" Addes edges from list of nodes (weight == 1) (for list [a b c] adds (a,b) (a,c) and (b,c) """
def edgesFromNodes(nodesList):
    tempEdges = itertools.combinations(nodesList, 2)
    for x in tempEdges:
        addNode(x[0],x[1])

counter = 1
for line in fileinput.input(["userHistoriesFixed.txt"]):
    history = line.rstrip('\n').split()
    edgesFromNodes(history)
    print(str(counter) + "..." + str(G.number_of_nodes()) + "..." + str(G.number_of_edges()))
    counter += 1
nx.write_weighted_edgelist(G, 'testGraph.txt')
