/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.thegreenavenger.dm.common;

/**
 *
 * @author David Thomas
 */
public class BasicScore implements Score, Comparable<BasicScore> {
    private float score;
    private int id;
    public BasicScore(float score, int id) { 
        this.score = score; 
        this.id = id;
    }
    public void add(double ns) { score+=ns; }
    public void divide(int count)
    {
        score/=(1.0*count);
    }
    public void divide(double count)
    {
        score/=(1.0*count);
    }
    public void set(float ns) { score = ns; }
    @Override
    public float get() { return score; }
    public int getID() { return id;}

    @Override
    public int compareTo(BasicScore o) {
        return new Float(score).compareTo(o.score);
    }
}
