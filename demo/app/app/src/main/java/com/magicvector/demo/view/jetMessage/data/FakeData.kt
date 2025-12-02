/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.magicvector.demo.view.jetMessage.data

import com.magicvector.demo.view.jetMessage.ConversationUiState
import com.magicvector.demo.view.jetMessage.Message


val initialMessages = listOf(
    Message(
        "me",
        "Check it out!",
        "8:07 PM",
    ),
    Message(
        "John Glenn",
        "You can use all the same stuff",
        "8:05 PM",
    ),
    Message(
        "Taylor Brooks",
        "@aliconors Take a look at the `Flow.collectAsStateWithLifecycle()` APIs",
        "8:05 PM",
    ),
    Message(
        "John Glenn",
        "Compose newbie as well, have you looked at the JetNews sample? " +
            "Most blog posts end up out of date pretty fast but this sample is always up to " +
            "date and deals with async data loading (it's faked but the same idea " +
            "applies) https://goo.gle/jetnews",
        "8:04 PM",
    ),
    Message(
        "me",
        "Compose newbie: I’ve scourged the internet for tutorials about async data " +
            "loading but haven’t found any good ones . " +
            "What’s the recommended way to load async data and emit composable widgets?",
        "8:03 PM",
    ),
    Message(
        "Shangeeth Sivan",
        "Does anyone know about Glance Widgets its the new way to build widgets in Android!",
        "8:08 PM",
    ),
    Message(
        "Taylor Brooks",
        "Wow! I never knew about Glance Widgets when was this added to the android ecosystem",
        "8:10 PM",
    ),
    Message(
        "John Glenn",
        "Yeah its seems to be pretty new!",
        "8:12 PM",
    ),
)

val exampleUiState = ConversationUiState(
    initialMessages = initialMessages,
    channelName = "#composers",
    channelMembers = 42,
)