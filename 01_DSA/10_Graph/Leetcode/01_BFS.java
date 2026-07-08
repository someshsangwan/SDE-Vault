public ArrayList<Integer> bfs(ArrayList<ArrayList<Integer>> adj) {
    int v  = adj.size();
    Queue<Integer> q = new LinkedList<>();
    ArrayList<Integer> ans = new ArrayList<>();
    boolean[] vist = new boolean[v];
    q.add(0);
    vist[0]  = true;
    while(!q.isEmpty()){
        int node = q.poll();
        ans.add(node);
        for(int i = 0; i < adj.get(node).size(); i++){
            int nei = adj.get(node).get(i);
            if(!vist[nei]){
                vist[nei] = true;
                q.add(nei);
            }
        }
    }
    return ans;

}