package com.example.rocketreserver

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.exception.ApolloException
import com.example.rocketreserver.databinding.LaunchListFragmentBinding
import kotlinx.coroutines.channels.Channel
import androidx.navigation.fragment.findNavController

class LaunchListFragment : Fragment() {
    private lateinit var binding: LaunchListFragmentBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = LaunchListFragmentBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        lifecycleScope.launchWhenResumed {
//            val response = apolloClient.query(LaunchListQuery()).await()
//
//            Log.d("LaunchList", "Success ${response?.data}")
//        }

        val launches = mutableListOf<LaunchListQuery.Launch>()
        val adapter = LaunchListAdapter(launches)
        binding.launches.layoutManager = LinearLayoutManager(requireContext())
        binding.launches.adapter = adapter

        val channel = Channel<Unit>(Channel.CONFLATED)

        // offer a first item to do the initial load else the list will stay empty forever
        channel.offer(Unit)
        adapter.onEndOfListReached = {
            channel.offer(Unit)
        }

        lifecycleScope.launchWhenResumed {
            var cursor: String? = null
            for (item in channel) {

                val response = try {
                    apolloClient.query(LaunchListQuery(cursor = Input.fromNullable(cursor))).await()
                } catch (e: ApolloException) {
                    Log.d("LaunchList", "Failure", e)
                    return@launchWhenResumed
                }

                            Log.d("LaunchList", "Success ${response?.data}")

                val newLaunches = response.data?.launches?.launches?.filterNotNull()

                if (newLaunches != null) {
                    launches.addAll(newLaunches)
                    adapter.notifyDataSetChanged()
                }

                cursor = response.data?.launches?.cursor
                if (response.data?.launches?.hasMore != true) {
                    break
                }
            }

            adapter.onEndOfListReached = null
            channel.close()

//
//            val launches = response?.data?.launches?.launches?.filterNotNull()
//            if (launches != null && !response.hasErrors()) {
//                val adapter = LaunchListAdapter(launches)
//                binding.launches.layoutManager = LinearLayoutManager(requireContext())
//                binding.launches.adapter = adapter
//            }

        }


        adapter.onItemClicked = { launch ->
            findNavController().navigate(
                LaunchListFragmentDirections.openLaunchDetails(launchId = launch.id)
            )
        }

    }



}