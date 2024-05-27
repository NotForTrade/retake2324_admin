package com.example.retake2324.data

class DbAccessException(message: String) : Exception(message)
class EmptyDbListResultException(message: String) : Exception(message)
class NullActiveDbObjectException(message: String) : Exception(message)